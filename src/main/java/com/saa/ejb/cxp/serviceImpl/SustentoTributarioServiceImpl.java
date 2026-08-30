package com.saa.ejb.cxp.serviceImpl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.ejb.cxp.dao.LiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.dao.NotaCreditoCompraDaoService;
import com.saa.ejb.cxp.dao.NotaDebitoCompraDaoService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.FacturaSustentoPendiente;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.rubros.SustentoTributarioSri;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * Implementacion de SustentoTributarioService. Ver el javadoc de la interfaz para la regla
 * de resolucion (decision de negocio) y la garantia de no pisar una resolucion existente.
 */
@Stateless
public class SustentoTributarioServiceImpl implements SustentoTributarioService {

    /**
     * Codigos que GRPPCSUS puede aportar como EXCEPCION a la regla base del IVA (corregido
     * 2026-08-27: un grupo mezcla facturas con y sin IVA -ej. "Servicios Basicos" trae luz y
     * agua al 0%-, asi que GRPPCSUS ya no puede ser el defecto general, solo estos tres casos
     * que el IVA de la factura no decide por si solo).
     */
    private static final List<String> CODIGOS_EXCEPCION_GRUPO = Arrays.asList(
            SustentoTributarioSri.ACTIVO_FIJO_CREDITO_IVA,
            SustentoTributarioSri.ACTIVO_FIJO_COSTO_GASTO_IR,
            SustentoTributarioSri.INVENTARIO_CREDITO_IVA,
            SustentoTributarioSri.INVENTARIO_COSTO_GASTO_IR,
            SustentoTributarioSri.REEMBOLSO_GASTO_INTERMEDIARIO);

    @PersistenceContext
    private EntityManager em;

    @EJB
    private FacturaCompraDaoService facturaCompraDaoService;

    @EJB
    private LiquidacionCompraCompraDaoService liquidacionCompraCompraDaoService;

    @EJB
    private NotaCreditoCompraDaoService notaCreditoCompraDaoService;

    @EJB
    private NotaDebitoCompraDaoService notaDebitoCompraDaoService;

    /* (non-Javadoc)
     * @see com.saa.ejb.cxp.service.SustentoTributarioService#calcularSustento(java.lang.Long)
     */
    @Override
    public String calcularSustento(Long idFactura) throws Throwable {
        if (idFactura == null) {
            return null;
        }

        // 1) Excepcion: si el grupo con mayor base imponible de la factura tiene configurado
        // uno de los tres codigos de excepcion (activo fijo / inventario / reembolso), ese
        // gana sobre la regla base del IVA. Un grupo sin excepcion configurada, o configurado
        // con un codigo que no es excepcion, simplemente no compite aqui.
        String porExcepcionDeGrupo = calcularSustentoPorExcepcionDeGrupo(idFactura);
        if (porExcepcionDeGrupo != null) {
            return porExcepcionDeGrupo;
        }

        // 2) Regla base: el sustento lo decide el IVA de la FACTURA, no el grupo. Verificado
        // el 2026-08-27 contra las 131 facturas de compra activas: 103 con IVA -> 01, 28 sin
        // IVA -> 02, cero sin resolver. Antes de esta correccion se intentaba resolver por
        // GRPPCSUS como defecto general, y daba 131/131 sin resolver -ver
        // docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md #6.2 para el porque-.
        FacturaCompra factura = facturaCompraDaoService.selectById(idFactura,
                NombreEntidadesCompra.FACTURA_COMPRA);
        if (factura == null) {
            return null;
        }
        boolean tieneIva = factura.getvIVA() != null && factura.getvIVA().doubleValue() > 0D;
        return tieneIva ? SustentoTributarioSri.CREDITO_TRIBUTARIO_IVA : SustentoTributarioSri.COSTO_GASTO_IR;
    }

    /**
     * Resuelve el sustento por la EXCEPCION de grupo de producto (activo fijo / inventario /
     * reembolso), o null si ninguna linea de la factura pertenece a un grupo con uno de esos
     * tres codigos configurado.
     *
     * @param idFactura		: Id de la factura de compra
     * @return				: Codigo de excepcion ganador, o null
     * @throws Throwable	: Excepcion
     */
    private String calcularSustentoPorExcepcionDeGrupo(Long idFactura) throws Throwable {
        // p.id = df.producto (no p = df.producto): DetalleFacturaCompra.producto es un Long
        // plano, no una relacion mapeada -mismo patron ya usado en
        // ClasificacionProductosLoteServiceImpl para el mismo join-.
        List<Object[]> filas = em.createQuery(
                "select p.grupoProducto.sustentoTributarioDefecto, sum(df.baseImponible) "
                        + "from DetalleFacturaCompra df, ProductoPago p "
                        + "where p.id = df.producto "
                        + "and df.factura.id = :idFactura "
                        + "and p.grupoProducto.sustentoTributarioDefecto in (:excepciones) "
                        + "group by p.grupoProducto.codigo, p.grupoProducto.sustentoTributarioDefecto "
                        + "order by sum(df.baseImponible) desc",
                Object[].class)
                .setParameter("idFactura", idFactura)
                .setParameter("excepciones", CODIGOS_EXCEPCION_GRUPO)
                .setMaxResults(1)
                .getResultList();
        if (filas.isEmpty()) {
            return null;
        }
        return (String) filas.get(0)[0];
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.cxp.service.SustentoTributarioService#resolverSiFalta(com.saa.model.cxp.FacturaCompra)
     */
    @Override
    public String resolverSiFalta(FacturaCompra factura) throws Throwable {
        if (factura == null || factura.getId() == null) {
            return null;
        }
        if (factura.getSustentoTributario() != null) {
            // Ya tiene valor -resuelto antes o corregido a mano-: no se toca.
            return factura.getSustentoTributario();
        }
        String resuelto = calcularSustento(factura.getId());
        if (resuelto == null) {
            return null;
        }
        factura.setSustentoTributario(resuelto);
        facturaCompraDaoService.save(factura, factura.getId());
        System.out.println("SustentoTributarioService: factura " + factura.getId()
                + " resuelta automaticamente con codSustento=" + resuelto);
        return resuelto;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.cxp.service.SustentoTributarioService#corregirSustento(java.lang.Long, java.lang.String)
     */
    @Override
    public FacturaCompra corregirSustento(Long idFactura, String sustento) throws Throwable {
        if (!esCodigoVigente(sustento)) {
            throw new IncomeException("Codigo de sustento tributario '" + sustento + "' invalido o"
                    + " inactivo. Debe ser uno de los codigos vigentes del catalogo SRI (PGS.TSRI"
                    + " donde PGS.LSRI.TABLA='" + SustentoTributarioSri.LSRI_TABLA + "').");
        }
        FacturaCompra factura = facturaCompraDaoService.selectById(idFactura,
                NombreEntidadesCompra.FACTURA_COMPRA);
        if (factura == null) {
            throw new IncomeException("No existe la factura de compra " + idFactura + ".");
        }
        String anterior = factura.getSustentoTributario();
        factura.setSustentoTributario(sustento);
        factura = facturaCompraDaoService.save(factura, factura.getId());
        System.out.println("SustentoTributarioService: factura " + idFactura
                + " corregida a mano: " + anterior + " -> " + sustento);
        return factura;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.cxp.service.SustentoTributarioService#listarPendientes(java.lang.Long)
     */
    @Override
    public List<FacturaSustentoPendiente> listarPendientes(Long idEmpresa) throws Throwable {
        List<FacturaCompra> facturas = facturaCompraDaoService.selectPendientesSustento(idEmpresa);
        List<FacturaSustentoPendiente> resultado = new java.util.ArrayList<FacturaSustentoPendiente>();
        for (FacturaCompra factura : facturas) {
            FacturaSustentoPendiente dto = new FacturaSustentoPendiente();
            dto.setId(factura.getId());
            dto.setNumero(factura.getNumero());
            dto.setFecha(factura.getFecha() != null ? factura.getFecha().toLocalDate() : null);
            if (factura.getTitular() != null) {
                dto.setProveedor(factura.getTitular().getNombre());
                dto.setIdentificacion(factura.getTitular().getIdentificacion());
            }
            dto.setTotal(factura.getTotal());
            dto.setIva(factura.getvIVA());
            dto.setSustentoSugerido(calcularSustento(factura.getId()));
            resultado.add(dto);
        }
        return resultado;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.cxp.service.SustentoTributarioService#catalogoVigente()
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> catalogoVigente() throws Throwable {
        List<Object[]> filas = em.createNativeQuery(
                "select t.codigo, t.detalle from PGS.TSRI t join PGS.LSRI l on t.lsri = l.id "
                        + "where l.tabla = ? and t.estado = 1 order by t.codigo")
                .setParameter(1, SustentoTributarioSri.LSRI_TABLA)
                .getResultList();
        Map<String, String> catalogo = new LinkedHashMap<String, String>();
        for (Object[] fila : filas) {
            catalogo.put((String) fila[0], (String) fila[1]);
        }
        return catalogo;
    }

    /**
     * Verifica que el código exista y esté activo en el catálogo real (PGS.LSRI/PGS.TSRI).
     *
     * <p><b>Por qué SQL nativo y no JPQL navegando la relación mapeada:</b>
     * <code>TsriCompra.lsri</code> está anotado <code>@JoinColumn(referencedColumnName =
     * "TABLA")</code>, pero los datos reales en <code>PGS.TSRI.LSRI</code> guardan el
     * <b>id numérico</b> de <code>PGS.LSRI</code> (ej. 27), no el texto de
     * <code>LSRI.TABLA</code> (ej. "703") — verificado contra la base local. Navegar
     * <code>t.lsri.tabla</code> en JPQL usa esa metadata rota y no encuentra ninguna fila.
     * Es un problema preexistente del mapeo, ajeno a este cambio: se reporta aquí y se
     * evita con SQL nativo sobre el join real (<code>TSRI.LSRI = LSRI.ID</code>), sin
     * tocar la entidad -que sirven otros módulos y no se validó su impacto-.</p>
     *
     * @param codigo	: Código a verificar
     * @return			: true si existe activo en el catálogo
     */
    private boolean esCodigoVigente(String codigo) {
        if (codigo == null || codigo.length() != 2) {
            return false;
        }
        Number total = (Number) em.createNativeQuery(
                "select count(*) from PGS.TSRI t join PGS.LSRI l on t.lsri = l.id "
                        + "where l.tabla = ? and t.codigo = ? and t.estado = 1")
                .setParameter(1, SustentoTributarioSri.LSRI_TABLA)
                .setParameter(2, codigo)
                .getSingleResult();
        return total.longValue() > 0;
    }

    // ========================================================================
    // Extensión a LQCC/NTCC/NTDC (2026-08-28). Misma regla que calcularSustento:
    // excepción por grupo de producto (mayor base imponible), si no la regla base
    // por IVA del documento. Ver docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §9.
    // ========================================================================

    @Override
    public String calcularSustentoLiquidacion(Long idLiquidacion) throws Throwable {
        if (idLiquidacion == null) {
            return null;
        }
        String porExcepcion = calcularSustentoPorExcepcionDeGrupoLiquidacion(idLiquidacion);
        if (porExcepcion != null) {
            return porExcepcion;
        }
        LiquidacionCompraCompra liquidacion = liquidacionCompraCompraDaoService.selectById(
                idLiquidacion, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
        if (liquidacion == null) {
            return null;
        }
        boolean tieneIva = liquidacion.getvIVA() != null && liquidacion.getvIVA().doubleValue() > 0D;
        return tieneIva ? SustentoTributarioSri.CREDITO_TRIBUTARIO_IVA : SustentoTributarioSri.COSTO_GASTO_IR;
    }

    /**
     * {@code DetalleLiquidacionCompraCompra.producto} ya es {@code @ManyToOne ProductoPago}
     * (a diferencia de factura/nota de crédito, que lo tienen como {@code Long} plano) —
     * join directo, sin la comparación manual {@code p.id = df.producto}. No hay
     * {@code baseImponible} en esta tabla: se pesa por {@code subTotal}, el equivalente.
     */
    private String calcularSustentoPorExcepcionDeGrupoLiquidacion(Long idLiquidacion) throws Throwable {
        List<Object[]> filas = em.createQuery(
                "select df.producto.grupoProducto.sustentoTributarioDefecto, sum(df.subTotal) "
                        + "from DetalleLiquidacionCompraCompra df "
                        + "where df.liquidacion.id = :idLiquidacion "
                        + "and df.producto is not null "
                        + "and df.producto.grupoProducto.sustentoTributarioDefecto in (:excepciones) "
                        + "group by df.producto.grupoProducto.codigo, df.producto.grupoProducto.sustentoTributarioDefecto "
                        + "order by sum(df.subTotal) desc",
                Object[].class)
                .setParameter("idLiquidacion", idLiquidacion)
                .setParameter("excepciones", CODIGOS_EXCEPCION_GRUPO)
                .setMaxResults(1)
                .getResultList();
        if (filas.isEmpty()) {
            return null;
        }
        return (String) filas.get(0)[0];
    }

    @Override
    public String resolverSiFaltaLiquidacion(LiquidacionCompraCompra liquidacion) throws Throwable {
        if (liquidacion == null || liquidacion.getId() == null) {
            return null;
        }
        if (liquidacion.getSustentoTributario() != null) {
            return liquidacion.getSustentoTributario();
        }
        String resuelto = calcularSustentoLiquidacion(liquidacion.getId());
        if (resuelto == null) {
            return null;
        }
        liquidacion.setSustentoTributario(resuelto);
        liquidacionCompraCompraDaoService.save(liquidacion, liquidacion.getId());
        System.out.println("SustentoTributarioService: liquidacion " + liquidacion.getId()
                + " resuelta automaticamente con codSustento=" + resuelto);
        return resuelto;
    }

    @Override
    public LiquidacionCompraCompra corregirSustentoLiquidacion(Long idLiquidacion, String sustento)
            throws Throwable {
        if (!esCodigoVigente(sustento)) {
            throw new IncomeException("Codigo de sustento tributario '" + sustento + "' invalido o"
                    + " inactivo. Debe ser uno de los codigos vigentes del catalogo SRI (PGS.TSRI"
                    + " donde PGS.LSRI.TABLA='" + SustentoTributarioSri.LSRI_TABLA + "').");
        }
        LiquidacionCompraCompra liquidacion = liquidacionCompraCompraDaoService.selectById(
                idLiquidacion, NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
        if (liquidacion == null) {
            throw new IncomeException("No existe la liquidacion de compra " + idLiquidacion + ".");
        }
        String anterior = liquidacion.getSustentoTributario();
        liquidacion.setSustentoTributario(sustento);
        liquidacion = liquidacionCompraCompraDaoService.save(liquidacion, liquidacion.getId());
        System.out.println("SustentoTributarioService: liquidacion " + idLiquidacion
                + " corregida a mano: " + anterior + " -> " + sustento);
        return liquidacion;
    }

    @Override
    public List<FacturaSustentoPendiente> listarPendientesLiquidacion(Long idEmpresa) throws Throwable {
        List<LiquidacionCompraCompra> liquidaciones =
                liquidacionCompraCompraDaoService.selectPendientesSustento(idEmpresa);
        List<FacturaSustentoPendiente> resultado = new java.util.ArrayList<FacturaSustentoPendiente>();
        for (LiquidacionCompraCompra liquidacion : liquidaciones) {
            FacturaSustentoPendiente dto = new FacturaSustentoPendiente();
            dto.setId(liquidacion.getId());
            dto.setNumero(liquidacion.getNumero());
            dto.setFecha(liquidacion.getFecha() != null ? liquidacion.getFecha().toLocalDate() : null);
            if (liquidacion.getTitular() != null) {
                dto.setProveedor(liquidacion.getTitular().getNombre());
                dto.setIdentificacion(liquidacion.getTitular().getIdentificacion());
            }
            dto.setTotal(liquidacion.getTotal());
            dto.setIva(liquidacion.getvIVA());
            dto.setSustentoSugerido(calcularSustentoLiquidacion(liquidacion.getId()));
            resultado.add(dto);
        }
        return resultado;
    }

    @Override
    public String calcularSustentoNotaCredito(Long idNotaCredito) throws Throwable {
        if (idNotaCredito == null) {
            return null;
        }
        String porExcepcion = calcularSustentoPorExcepcionDeGrupoNotaCredito(idNotaCredito);
        if (porExcepcion != null) {
            return porExcepcion;
        }
        NotaCreditoCompra notaCredito = notaCreditoCompraDaoService.selectById(
                idNotaCredito, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
        if (notaCredito == null) {
            return null;
        }
        boolean tieneIva = notaCredito.getvIVA() != null && notaCredito.getvIVA().doubleValue() > 0D;
        return tieneIva ? SustentoTributarioSri.CREDITO_TRIBUTARIO_IVA : SustentoTributarioSri.COSTO_GASTO_IR;
    }

    /**
     * {@code DetalleNotaCreditoCompra.producto} es {@code Long} plano (igual que
     * {@code DetalleFacturaCompra}) — mismo patrón de join manual {@code p.id = df.producto}.
     * Sí tiene {@code baseImponible}, igual que factura.
     */
    private String calcularSustentoPorExcepcionDeGrupoNotaCredito(Long idNotaCredito) throws Throwable {
        List<Object[]> filas = em.createQuery(
                "select p.grupoProducto.sustentoTributarioDefecto, sum(df.baseImponible) "
                        + "from DetalleNotaCreditoCompra df, ProductoPago p "
                        + "where p.id = df.producto "
                        + "and df.notaCredito.id = :idNotaCredito "
                        + "and p.grupoProducto.sustentoTributarioDefecto in (:excepciones) "
                        + "group by p.grupoProducto.codigo, p.grupoProducto.sustentoTributarioDefecto "
                        + "order by sum(df.baseImponible) desc",
                Object[].class)
                .setParameter("idNotaCredito", idNotaCredito)
                .setParameter("excepciones", CODIGOS_EXCEPCION_GRUPO)
                .setMaxResults(1)
                .getResultList();
        if (filas.isEmpty()) {
            return null;
        }
        return (String) filas.get(0)[0];
    }

    @Override
    public String resolverSiFaltaNotaCredito(NotaCreditoCompra notaCredito) throws Throwable {
        if (notaCredito == null || notaCredito.getId() == null) {
            return null;
        }
        if (notaCredito.getSustentoTributario() != null) {
            return notaCredito.getSustentoTributario();
        }
        String resuelto = calcularSustentoNotaCredito(notaCredito.getId());
        if (resuelto == null) {
            return null;
        }
        notaCredito.setSustentoTributario(resuelto);
        notaCreditoCompraDaoService.save(notaCredito, notaCredito.getId());
        System.out.println("SustentoTributarioService: nota de credito " + notaCredito.getId()
                + " resuelta automaticamente con codSustento=" + resuelto);
        return resuelto;
    }

    @Override
    public NotaCreditoCompra corregirSustentoNotaCredito(Long idNotaCredito, String sustento)
            throws Throwable {
        if (!esCodigoVigente(sustento)) {
            throw new IncomeException("Codigo de sustento tributario '" + sustento + "' invalido o"
                    + " inactivo. Debe ser uno de los codigos vigentes del catalogo SRI (PGS.TSRI"
                    + " donde PGS.LSRI.TABLA='" + SustentoTributarioSri.LSRI_TABLA + "').");
        }
        NotaCreditoCompra notaCredito = notaCreditoCompraDaoService.selectById(
                idNotaCredito, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
        if (notaCredito == null) {
            throw new IncomeException("No existe la nota de credito de compra " + idNotaCredito + ".");
        }
        String anterior = notaCredito.getSustentoTributario();
        notaCredito.setSustentoTributario(sustento);
        notaCredito = notaCreditoCompraDaoService.save(notaCredito, notaCredito.getId());
        System.out.println("SustentoTributarioService: nota de credito " + idNotaCredito
                + " corregida a mano: " + anterior + " -> " + sustento);
        return notaCredito;
    }

    @Override
    public List<FacturaSustentoPendiente> listarPendientesNotaCredito(Long idEmpresa) throws Throwable {
        List<NotaCreditoCompra> notas = notaCreditoCompraDaoService.selectPendientesSustento(idEmpresa);
        List<FacturaSustentoPendiente> resultado = new java.util.ArrayList<FacturaSustentoPendiente>();
        for (NotaCreditoCompra nota : notas) {
            FacturaSustentoPendiente dto = new FacturaSustentoPendiente();
            dto.setId(nota.getId());
            dto.setNumero(nota.getNumero());
            dto.setFecha(nota.getFecha() != null ? nota.getFecha().toLocalDate() : null);
            if (nota.getTitular() != null) {
                dto.setProveedor(nota.getTitular().getNombre());
                dto.setIdentificacion(nota.getTitular().getIdentificacion());
            }
            dto.setTotal(nota.getTotal());
            dto.setIva(nota.getvIVA());
            dto.setSustentoSugerido(calcularSustentoNotaCredito(nota.getId()));
            resultado.add(dto);
        }
        return resultado;
    }

    @Override
    public String calcularSustentoNotaDebito(Long idNotaDebito) throws Throwable {
        // Sin paso de excepcion por grupo: DetalleNotaDebitoCompra no tiene columna de
        // producto (verificado 2026-08-28) -ver el javadoc de la interfaz-. Siempre regla
        // base del IVA del documento.
        if (idNotaDebito == null) {
            return null;
        }
        NotaDebitoCompra notaDebito = notaDebitoCompraDaoService.selectById(
                idNotaDebito, NombreEntidadesCompra.NOTA_DEBITO_COMPRA);
        if (notaDebito == null) {
            return null;
        }
        boolean tieneIva = notaDebito.getvIVA() != null && notaDebito.getvIVA().doubleValue() > 0D;
        return tieneIva ? SustentoTributarioSri.CREDITO_TRIBUTARIO_IVA : SustentoTributarioSri.COSTO_GASTO_IR;
    }

    @Override
    public String resolverSiFaltaNotaDebito(NotaDebitoCompra notaDebito) throws Throwable {
        if (notaDebito == null || notaDebito.getId() == null) {
            return null;
        }
        if (notaDebito.getSustentoTributario() != null) {
            return notaDebito.getSustentoTributario();
        }
        String resuelto = calcularSustentoNotaDebito(notaDebito.getId());
        if (resuelto == null) {
            return null;
        }
        notaDebito.setSustentoTributario(resuelto);
        notaDebitoCompraDaoService.save(notaDebito, notaDebito.getId());
        System.out.println("SustentoTributarioService: nota de debito " + notaDebito.getId()
                + " resuelta automaticamente con codSustento=" + resuelto);
        return resuelto;
    }

    @Override
    public NotaDebitoCompra corregirSustentoNotaDebito(Long idNotaDebito, String sustento)
            throws Throwable {
        if (!esCodigoVigente(sustento)) {
            throw new IncomeException("Codigo de sustento tributario '" + sustento + "' invalido o"
                    + " inactivo. Debe ser uno de los codigos vigentes del catalogo SRI (PGS.TSRI"
                    + " donde PGS.LSRI.TABLA='" + SustentoTributarioSri.LSRI_TABLA + "').");
        }
        NotaDebitoCompra notaDebito = notaDebitoCompraDaoService.selectById(
                idNotaDebito, NombreEntidadesCompra.NOTA_DEBITO_COMPRA);
        if (notaDebito == null) {
            throw new IncomeException("No existe la nota de debito de compra " + idNotaDebito + ".");
        }
        String anterior = notaDebito.getSustentoTributario();
        notaDebito.setSustentoTributario(sustento);
        notaDebito = notaDebitoCompraDaoService.save(notaDebito, notaDebito.getId());
        System.out.println("SustentoTributarioService: nota de debito " + idNotaDebito
                + " corregida a mano: " + anterior + " -> " + sustento);
        return notaDebito;
    }

    @Override
    public List<FacturaSustentoPendiente> listarPendientesNotaDebito(Long idEmpresa) throws Throwable {
        List<NotaDebitoCompra> notas = notaDebitoCompraDaoService.selectPendientesSustento(idEmpresa);
        List<FacturaSustentoPendiente> resultado = new java.util.ArrayList<FacturaSustentoPendiente>();
        for (NotaDebitoCompra nota : notas) {
            FacturaSustentoPendiente dto = new FacturaSustentoPendiente();
            dto.setId(nota.getId());
            dto.setNumero(nota.getNumero());
            dto.setFecha(nota.getFecha() != null ? nota.getFecha().toLocalDate() : null);
            if (nota.getTitular() != null) {
                dto.setProveedor(nota.getTitular().getNombre());
                dto.setIdentificacion(nota.getTitular().getIdentificacion());
            }
            dto.setTotal(nota.getTotal());
            dto.setIva(nota.getvIVA());
            dto.setSustentoSugerido(calcularSustentoNotaDebito(nota.getId()));
            resultado.add(dto);
        }
        return resultado;
    }

}
