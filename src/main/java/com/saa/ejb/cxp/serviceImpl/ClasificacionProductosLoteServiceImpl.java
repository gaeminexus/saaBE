package com.saa.ejb.cxp.serviceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.ProductoPagoDaoService;
import com.saa.ejb.cxp.service.ClasificacionProductosLoteService;
import com.saa.model.cxp.GrupoProductoPago;
import com.saa.model.cxp.ProductoPago;
import com.saa.rubros.TipoGrupoProductos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Implementación de la clasificación masiva de productos por carga TXT.
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Stateless
public class ClasificacionProductosLoteServiceImpl implements ClasificacionProductosLoteService {

    /**
     * Valor que graba el registro en {@code DCXPTBTD} cuando el documento fue a
     * PGS.FCTC. Es la única tabla destino con productos: notas, liquidaciones y
     * retenciones no pasan por ProductoPago.
     */
    private static final String TABLA_FACTURA_COMPRA = "FACTURA_COMPRA";

    @PersistenceContext
    private EntityManager em;

    @EJB private ProductoPagoDaoService productoPagoDaoService;

    // =========================================================
    // §6.5 — Qué está sin clasificar en esta carga
    // =========================================================

    @Override
    public Map<String, Object> productosSinClasificarLote(Long idCargaTxt) throws Throwable {

        System.out.println("=== productosSinClasificarLote idCargaTxt=" + idCargaTxt);

        // Documentos de la carga que ya tienen factura de compra creada. Los que
        // todavía no llegaron a registrarse no tienen ni detalle ni sustentos, así
        // que no pueden aportar productos.
        @SuppressWarnings("unchecked")
        List<Object[]> facturas = em.createQuery(
                "select doc.idDocumentoBD, doc.serieComprobante, doc.claveAcceso "
                + "  from DocumentoCxp doc "
                + " where exists (select 1 from DetalleCargaTxt d "
                + "                where d.documento = doc and d.cargaTxt.id = :idCarga) "
                + "   and doc.tipoTablaDestino = :tablaDestino "
                + "   and doc.idDocumentoBD is not null "
                + " order by doc.id")
                .setParameter("idCarga", idCargaTxt)
                .setParameter("tablaDestino", TABLA_FACTURA_COMPRA)
                .getResultList();

        // Acumulador por producto: un mismo producto suele aparecer en varias
        // facturas de la misma carga y tiene que salir una sola vez, con la lista
        // de los comprobantes que lo usan. LinkedHashMap para que el orden de
        // salida sea el de aparición y la pantalla no baile entre consultas.
        Map<Long, Map<String, Object>> acumulado = new LinkedHashMap<>();

        for (Object[] fila : facturas) {
            Long idFacturaCompra = (Long) fila[0];
            String etiqueta = fila[1] != null ? (String) fila[1] : (String) fila[2];

            for (ProductoPago producto : productosSinClasificarDeFactura(idFacturaCompra)) {
                Map<String, Object> entrada = acumulado.get(producto.getId());
                if (entrada == null) {
                    entrada = new LinkedHashMap<>();
                    entrada.put("id", producto.getId());
                    entrada.put("nombre", producto.getNombre());
                    entrada.put("codigo", producto.getCodigo());
                    entrada.put("grupoActual", producto.getGrupoProducto() != null
                            ? producto.getGrupoProducto().getNombre() : null);
                    entrada.put("documentos", new ArrayList<String>());
                    acumulado.put(producto.getId(), entrada);
                }
                @SuppressWarnings("unchecked")
                List<String> documentos = (List<String>) entrada.get("documentos");
                if (etiqueta != null && !documentos.contains(etiqueta))
                    documentos.add(etiqueta);
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("idCargaTxt", idCargaTxt);
        resultado.put("productos", new ArrayList<Map<String, Object>>(acumulado.values()));

        System.out.println("=== productosSinClasificarLote facturas=" + facturas.size()
                + " productosPendientes=" + acumulado.size());
        return resultado;
    }

    /**
     * Productos en POR CLASIFICAR de UNA factura de compra.
     *
     * <p>
     * <b>La regla del reembolso.</b> Si la factura está marcada
     * {@code esReembolso = 1}, los productos que importan son los de los
     * documentos sustento (PGS.RMBF), no los del detalle (PGS.DFCC): en una
     * factura de reembolso el detalle no participa del asiento, y por eso el
     * registro tampoco lo exige clasificado. Es exactamente la misma bifurcación
     * de {@code ProcesoCargaDocumentosServiceImpl.obtenerProductosPendientesDeClasificar}.
     * </p>
     *
     * <p>
     * <b>Por qué no se invoca aquel método directamente</b>: devuelve
     * {@code List<String>}, y en la rama de DFCC lo que devuelve es
     * {@code df.descripcion} —el texto de la línea, no el producto—, así que no
     * hay forma de sacar de ahí los {@code id} que §6.5 tiene que publicar.
     * Volver del nombre al producto sería adivinar. Si la regla del reembolso
     * cambia alguna vez, hay que tocar los dos sitios.
     * </p>
     *
     * @param idFacturaCompra : Id de la FacturaCompra
     * @return                : Productos sin clasificar, sin repetir
     */
    private List<ProductoPago> productosSinClasificarDeFactura(Long idFacturaCompra) {

        @SuppressWarnings("unchecked")
        List<Long> esReembolsoList = em.createQuery(
                "select f.esReembolso from FacturaCompra f where f.id = :id")
                .setParameter("id", idFacturaCompra).setMaxResults(1).getResultList();
        boolean esReembolso = !esReembolsoList.isEmpty()
                && esReembolsoList.get(0) != null && esReembolsoList.get(0) == 1L;

        if (esReembolso) {
            @SuppressWarnings("unchecked")
            List<ProductoPago> pendientes = em.createQuery(
                    "select distinct p from ReembolsoFacturaCompra r, ProductoPago p, GrupoProductoPago g "
                    + "where p.id = r.producto and g.codigo = p.grupoProducto.codigo "
                    + "and r.factura.id = :idFactura and r.estado = 1 "
                    + "and g.rubroTipoGrupoH = :tipo")
                    .setParameter("idFactura", idFacturaCompra)
                    .setParameter("tipo", (long) TipoGrupoProductos.POR_CLASIFICAR)
                    .getResultList();
            return pendientes;
        }

        @SuppressWarnings("unchecked")
        List<ProductoPago> pendientes = em.createQuery(
                "select distinct p from DetalleFacturaCompra df, ProductoPago p, GrupoProductoPago g "
                + "where p.id = df.producto and g.codigo = p.grupoProducto.codigo "
                + "and df.factura.id = :idFactura "
                + "and g.rubroTipoGrupoH = :tipo")
                .setParameter("idFactura", idFacturaCompra)
                .setParameter("tipo", (long) TipoGrupoProductos.POR_CLASIFICAR)
                .getResultList();
        return pendientes;
    }

    // =========================================================
    // §6.4 — Asignar grupo en lote
    // =========================================================

    @Override
    public Map<String, Object> clasificarProductosLote(Long idEmpresa,
            List<Map<String, Object>> asignaciones) throws Throwable {

        System.out.println("=== clasificarProductosLote idEmpresa=" + idEmpresa
                + " asignaciones=" + (asignaciones != null ? asignaciones.size() : 0));

        if (idEmpresa == null)
            throw new IncomeException("Falta el idEmpresa.");
        if (asignaciones == null || asignaciones.isEmpty())
            throw new IncomeException("No se recibió ninguna asignación que aplicar.");

        // Un grupo se repite en casi todas las líneas del envío; se valida una vez.
        Map<Long, GrupoProductoPago> gruposValidados = new HashMap<>();
        List<Long> noEncontrados = new ArrayList<>();
        int actualizados = 0;

        for (Map<String, Object> asignacion : asignaciones) {
            Long idProducto = aLong(asignacion.get("idProducto"));
            Long idGrupo    = aLong(asignacion.get("idGrupo"));

            if (idProducto == null || idGrupo == null)
                throw new IncomeException("Cada asignación debe traer idProducto e idGrupo: "
                        + asignacion);

            GrupoProductoPago grupo = grupoValidado(idGrupo, idEmpresa, gruposValidados);

            // Producto inexistente: no corta el envío. El frontend armó la lista
            // desde §6.5 y entre esa consulta y este POST la fila pudo desaparecer.
            ProductoPago producto = em.find(ProductoPago.class, idProducto);
            if (producto == null) {
                System.out.println("   ⚠ producto " + idProducto + " no existe → noEncontrados");
                noEncontrados.add(idProducto);
                continue;
            }

            // Producto de otra empresa: esto sí corta. No es una fila que se
            // borró, es una mezcla de empresas, y con IncomeException se revierte
            // todo el envío en vez de dejar la mitad aplicada.
            if (producto.getEmpresa() != null
                    && !idEmpresa.equals(producto.getEmpresa().getCodigo()))
                throw new IncomeException("El producto " + idProducto + " ('"
                        + producto.getNombre() + "') pertenece a la empresa "
                        + producto.getEmpresa().getCodigo() + ", no a la " + idEmpresa + ".");

            producto.setGrupoProducto(grupo);
            productoPagoDaoService.save(producto, producto.getId());
            actualizados++;
            System.out.println("   ✓ producto " + idProducto + " ('" + producto.getNombre()
                    + "') → grupo " + idGrupo + " ('" + grupo.getNombre() + "')");
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("actualizados", actualizados);
        resultado.put("noEncontrados", noEncontrados);

        System.out.println("=== clasificarProductosLote actualizados=" + actualizados
                + " noEncontrados=" + noEncontrados);
        return resultado;
    }

    /**
     * Recupera el grupo y comprueba que sea de la empresa, con memoria para no
     * repetir la consulta por cada línea del envío.
     *
     * @param idGrupo    : Id del grupo destino
     * @param idEmpresa  : Empresa que hace la petición
     * @param validados  : Grupos ya comprobados en este envío
     * @return           : El grupo
     * @throws IncomeException : Si no existe o es de otra empresa
     */
    private GrupoProductoPago grupoValidado(Long idGrupo, Long idEmpresa,
            Map<Long, GrupoProductoPago> validados) {

        GrupoProductoPago grupo = validados.get(idGrupo);
        if (grupo != null) return grupo;

        grupo = em.find(GrupoProductoPago.class, idGrupo);
        if (grupo == null)
            throw new IncomeException("No existe el grupo de productos " + idGrupo + ".");

        if (grupo.getEmpresa() == null || !idEmpresa.equals(grupo.getEmpresa().getCodigo()))
            throw new IncomeException("El grupo " + idGrupo + " ('" + grupo.getNombre()
                    + "') no pertenece a la empresa " + idEmpresa + ".");

        validados.put(idGrupo, grupo);
        return grupo;
    }

    /**
     * Convierte a Long lo que venga del JSON. Jackson entrega los enteros como
     * Integer cuando caben, así que un cast directo a Long revienta.
     *
     * @param valor : Valor recibido en el body
     * @return      : El número, o null si no venía o no es numérico
     */
    private Long aLong(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).longValue();
        try {
            String texto = valor.toString().trim();
            return texto.isEmpty() ? null : Long.valueOf(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
