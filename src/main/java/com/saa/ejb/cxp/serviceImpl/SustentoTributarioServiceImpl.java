package com.saa.ejb.cxp.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.ejb.cxp.dao.LiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.dao.NotaCreditoCompraDaoService;
import com.saa.ejb.cxp.dao.NotaDebitoCompraDaoService;
import com.saa.ejb.cxp.service.SustentoTributarioService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.FacturaSustentoPendiente;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.model.cxp.ResultadoSustento;
import com.saa.rubros.Estado;
import com.saa.rubros.Rubros;
import com.saa.rubros.SustentoTributarioSri;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * Implementacion de SustentoTributarioService. Ver el javadoc de la interfaz para la regla
 * de resolucion (decision de negocio) y la garantia de no pisar una resolucion existente.
 *
 * <p><b>C37 / D-126 (2026-11-19):</b> la regla base dejo de ser solo el IVA. Con IVA, el
 * sustento lo decide el centro de costo del documento -el de las lineas de su asiento
 * (CNT.DTAS.CNCSCDGO), unico lugar donde el SAA lo guarda-: un centro del rubro
 * {@link Rubros#CXP_CENTROS_COSTO_CREDITO_TRIBUTARIO} da 01, cualquier otro 02. Sin IVA, 02.
 * Sin centro, la regla del IVA de antes (01) con aviso. Las excepciones por grupo de producto
 * (03/04/06/07/08) siguen ganando sobre todo lo anterior.</p>
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

    // ========================================================================
    // Regla D-126 (C37): evaluacion detallada, comun a los cuatro documentos
    // ========================================================================

    /* (non-Javadoc)
     * @see com.saa.ejb.cxp.service.SustentoTributarioService#evaluarSustento(java.lang.String, java.lang.Long)
     */
    @Override
    public ResultadoSustento evaluarSustento(String tipoDocumento, Long idDocumento) throws Throwable {
        if (tipoDocumento == null || idDocumento == null) {
            return null;
        }
        Documento d = cargarDocumento(tipoDocumento, idDocumento);
        if (d == null) {
            return null;
        }
        return evaluar(d);
    }

    /** Vista comun de los cuatro documentos de compra, para no repetir la regla cuatro veces. */
    private static final class Documento {
        String tipo;
        Long id;
        String numero;
        LocalDate fecha;
        Double iva;
        String guardado;
        Long idAsiento;
    }

    private Documento cargarDocumento(String tipo, Long id) throws Throwable {
        Documento d = new Documento();
        d.tipo = tipo;
        d.id = id;
        if (FACTURA.equals(tipo)) {
            FacturaCompra f = facturaCompraDaoService.selectById(id, NombreEntidadesCompra.FACTURA_COMPRA);
            if (f == null) return null;
            llenar(d, f.getNumero(), f.getFecha(), f.getvIVA(), f.getSustentoTributario(), f.getAsiento());
        } else if (LIQUIDACION.equals(tipo)) {
            LiquidacionCompraCompra l = liquidacionCompraCompraDaoService.selectById(id,
                    NombreEntidadesCompra.LIQUIDACION_COMPRA_COMPRA);
            if (l == null) return null;
            llenar(d, l.getNumero(), l.getFecha(), l.getvIVA(), l.getSustentoTributario(), l.getAsiento());
        } else if (NOTA_CREDITO.equals(tipo)) {
            NotaCreditoCompra n = notaCreditoCompraDaoService.selectById(id, NombreEntidadesCompra.NOTA_CREDITO_COMPRA);
            if (n == null) return null;
            llenar(d, n.getNumero(), n.getFecha(), n.getvIVA(), n.getSustentoTributario(), n.getAsiento());
        } else if (NOTA_DEBITO.equals(tipo)) {
            NotaDebitoCompra n = notaDebitoCompraDaoService.selectById(id, NombreEntidadesCompra.NOTA_DEBITO_COMPRA);
            if (n == null) return null;
            llenar(d, n.getNumero(), n.getFecha(), n.getvIVA(), n.getSustentoTributario(), n.getAsiento());
        } else {
            throw new IncomeException("Tipo de documento de compra '" + tipo + "' desconocido. Use "
                    + FACTURA + ", " + LIQUIDACION + ", " + NOTA_CREDITO + " o " + NOTA_DEBITO + ".");
        }
        return d;
    }

    private static void llenar(Documento d, String numero, LocalDateTime fecha, Double iva, String guardado,
            Asiento asiento) {
        d.numero = numero;
        d.fecha = fecha != null ? fecha.toLocalDate() : null;
        d.iva = iva;
        d.guardado = guardado;
        d.idAsiento = asiento != null ? asiento.getCodigo() : null;
    }

    /**
     * La regla completa, sin guardar: 1) excepcion por grupo de producto; 2) sin IVA -> 02;
     * 3) con IVA y centro de costo -> 01 si el centro esta en el rubro 251, si no 02; 4) con IVA y
     * sin centro (o sin el rubro) -> 01, la regla del IVA de antes, con aviso.
     */
    private ResultadoSustento evaluar(Documento d) throws Throwable {
        ResultadoSustento r = new ResultadoSustento();
        r.setTipoDocumento(d.tipo);
        r.setIdDocumento(d.id);
        r.setNumero(d.numero);
        r.setFecha(d.fecha);
        r.setIva(d.iva);
        r.setSustentoGuardado(d.guardado);

        String porExcepcion = excepcionDeGrupo(d.tipo, d.id);
        if (porExcepcion != null) {
            r.setSustento(porExcepcion);
            r.setRegla(ResultadoSustento.REGLA_EXCEPCION_GRUPO);
            return r;
        }

        // Centro de costo: se informa siempre, aunque sin IVA no decida nada.
        List<Object[]> centros = centrosDelAsiento(d.idAsiento);
        if (!centros.isEmpty()) {
            Object[] mayor = centros.get(0);
            r.setIdCentroCosto(((Number) mayor[0]).longValue());
            r.setCentroCosto(mayor[1] != null ? mayor[1].toString() : null);
            r.setCentroMixto(centros.size() > 1);
        }

        boolean tieneIva = d.iva != null && d.iva.doubleValue() > 0D;
        if (!tieneIva) {
            r.setSustento(SustentoTributarioSri.COSTO_GASTO_IR);
            r.setRegla(ResultadoSustento.REGLA_SIN_IVA);
            return r;
        }

        Set<Long> conCredito = centrosConCredito();
        if (conCredito == null) {
            r.setSustento(SustentoTributarioSri.CREDITO_TRIBUTARIO_IVA);
            r.setRegla(ResultadoSustento.REGLA_IVA_SIN_PARAMETRO);
            r.setAviso("No existe el rubro " + Rubros.CXP_CENTROS_COSTO_CREDITO_TRIBUTARIO + " (centros de costo con"
                    + " credito tributario, D-126): se aplico la regla del IVA (01). Correr la semilla C37-01.");
            return r;
        }
        if (r.getIdCentroCosto() == null) {
            r.setSustento(SustentoTributarioSri.CREDITO_TRIBUTARIO_IVA);
            r.setRegla(ResultadoSustento.REGLA_IVA_SIN_CENTRO);
            r.setAviso(etiqueta(d) + " con IVA y sin centro de costo en su asiento"
                    + (d.idAsiento == null ? " (todavia no tiene asiento)" : "")
                    + ": se aplico la regla del IVA (01). Con el centro, D-126 puede dar 02: revisar.");
            return r;
        }
        boolean credito = conCredito.contains(r.getIdCentroCosto());
        r.setSustento(credito ? SustentoTributarioSri.CREDITO_TRIBUTARIO_IVA : SustentoTributarioSri.COSTO_GASTO_IR);
        r.setRegla(credito ? ResultadoSustento.REGLA_CENTRO_CON_CREDITO : ResultadoSustento.REGLA_CENTRO_SIN_CREDITO);
        if (r.isCentroMixto()) {
            r.setAviso(etiqueta(d) + " tiene " + centros.size() + " centros de costo en su asiento: se tomo el de"
                    + " mayor valor (" + r.getCentroCosto() + "). Revisar.");
        }
        return r;
    }

    private static String etiqueta(Documento d) {
        String nombre = FACTURA.equals(d.tipo) ? "Factura de compra"
                : LIQUIDACION.equals(d.tipo) ? "Liquidacion de compra"
                : NOTA_CREDITO.equals(d.tipo) ? "Nota de credito de compra" : "Nota de debito de compra";
        return nombre + " " + d.id + (d.numero != null ? " (" + d.numero + ")" : "");
    }

    /**
     * Centros de costo de las lineas del asiento, de mayor a menor valor (debe + haber). En el SAA
     * el centro no vive en la cabecera del documento sino en las lineas de su asiento: ahi lo pone
     * la carga historica C11 y ahi lo midio D-126 (sql/02 de C37).
     *
     * @return filas {CNCSCDGO, CNCSNMBR, valor}; vacia si no hay asiento o ninguna linea tiene centro
     */
    @SuppressWarnings("unchecked")
    private List<Object[]> centrosDelAsiento(Long idAsiento) {
        if (idAsiento == null) {
            return new ArrayList<Object[]>();
        }
        return em.createNativeQuery(
                "SELECT d.CNCSCDGO, c.CNCSNMBR, SUM(ABS(NVL(d.DTASDBEE,0)) + ABS(NVL(d.DTASHBRR,0))) v "
                        + "FROM CNT.DTAS d JOIN CNT.CNCS c ON c.CNCSCDGO = d.CNCSCDGO "
                        + "WHERE d.ASNTCDGO = ?1 GROUP BY d.CNCSCDGO, c.CNCSNMBR ORDER BY 3 DESC, 1")
                .setParameter(1, idAsiento)
                .getResultList();
    }

    /**
     * Centros de costo con credito tributario (rubro 251, detalles vigentes, CNCSCDGO en PDTRVLRN).
     * El rubro se busca por su alterno, como el resto de parametros: en produccion el PRBRCDGO
     * puede no coincidir (paso con el 249).
     *
     * @return el conjunto (vacio si el rubro existe sin detalles vigentes), o null si el rubro no existe
     */
    @SuppressWarnings("unchecked")
    private Set<Long> centrosConCredito() {
        Number rubros = (Number) em.createNativeQuery("SELECT COUNT(*) FROM SCP.PRBR r WHERE r.PRBRALTR = ?1")
                .setParameter(1, Rubros.CXP_CENTROS_COSTO_CREDITO_TRIBUTARIO)
                .getSingleResult();
        if (rubros.longValue() == 0) {
            return null;
        }
        List<Object> filas = em.createNativeQuery(
                "SELECT d.PDTRVLRN FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO "
                        + "WHERE r.PRBRALTR = ?1 AND d.PDTRESTD = 1 AND d.PDTRVLRN IS NOT NULL")
                .setParameter(1, Rubros.CXP_CENTROS_COSTO_CREDITO_TRIBUTARIO)
                .getResultList();
        Set<Long> centros = new HashSet<Long>();
        for (Object f : filas) {
            centros.add(((Number) f).longValue());
        }
        return centros;
    }

    private String excepcionDeGrupo(String tipo, Long id) throws Throwable {
        if (FACTURA.equals(tipo)) return calcularSustentoPorExcepcionDeGrupo(id);
        if (LIQUIDACION.equals(tipo)) return calcularSustentoPorExcepcionDeGrupoLiquidacion(id);
        if (NOTA_CREDITO.equals(tipo)) return calcularSustentoPorExcepcionDeGrupoNotaCredito(id);
        // Nota de debito: su detalle no tiene producto (ver javadoc de la interfaz).
        return null;
    }

    /** Deja constancia en el log del servidor del aviso de la regla, si lo hay. */
    private static void registrarAviso(ResultadoSustento r) {
        if (r != null && r.getAviso() != null) {
            System.out.println("⚠ SustentoTributarioService (D-126): " + r.getAviso());
        }
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.cxp.service.SustentoTributarioService#verificarPeriodo(java.lang.Long, java.time.LocalDate, java.time.LocalDate)
     */
    @Override
    public Map<String, Object> verificarPeriodo(Long idEmpresa, LocalDate desde, LocalDate hasta) throws Throwable {
        if (idEmpresa == null || desde == null || hasta == null) {
            throw new IncomeException("Debe indicar la empresa y el rango de fechas (desde, hasta).");
        }
        // Mismo criterio de fecha que el ATS (GeneradorAtsServiceImpl): fecha de registro contable,
        // o la de emision si no la tiene.
        String[][] entidades = {
                { FACTURA, "FacturaCompra" }, { LIQUIDACION, "LiquidacionCompraCompra" },
                { NOTA_CREDITO, "NotaCreditoCompra" }, { NOTA_DEBITO, "NotaDebitoCompra" } };
        int total = 0, coinciden = 0, vacios = 0;
        Map<String, Integer> resumen = new TreeMap<String, Integer>();
        List<ResultadoSustento> diferencias = new ArrayList<ResultadoSustento>();
        List<ResultadoSustento> conAviso = new ArrayList<ResultadoSustento>();
        for (String[] e : entidades) {
            List<Long> ids = em.createQuery("select x.id from " + e[1] + " x where x.empresa.codigo = :e "
                    + "and x.estado = :activo "
                    + "and ((x.fechaRegistroContable between :desde and :hasta) "
                    + "or (x.fechaRegistroContable is null and x.fecha between :desdeDT and :hastaDT)) "
                    + "order by x.id", Long.class)
                    .setParameter("e", idEmpresa)
                    .setParameter("activo", Long.valueOf(Estado.ACTIVO))
                    .setParameter("desde", desde)
                    .setParameter("hasta", hasta)
                    .setParameter("desdeDT", desde.atStartOfDay())
                    .setParameter("hastaDT", hasta.atTime(23, 59, 59))
                    .getResultList();
            for (Long id : ids) {
                ResultadoSustento r = evaluarSustento(e[0], id);
                if (r == null) continue;
                total++;
                if (r.getSustentoGuardado() == null) vacios++;
                String clave = e[0] + " | " + r.getRegla() + " | guardado " + (r.getSustentoGuardado() == null ? "--"
                        : r.getSustentoGuardado()) + " -> regla " + r.getSustento();
                resumen.put(clave, resumen.containsKey(clave) ? resumen.get(clave) + 1 : 1);
                if (r.isCoincide()) {
                    coinciden++;
                } else {
                    diferencias.add(r);
                }
                if (r.getAviso() != null) {
                    conAviso.add(r);
                }
            }
        }
        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        resultado.put("idEmpresa", idEmpresa);
        resultado.put("desde", desde.toString());
        resultado.put("hasta", hasta.toString());
        resultado.put("centrosConCredito", centrosConCredito());
        resultado.put("documentos", total);
        resultado.put("coinciden", coinciden);
        resultado.put("sinSustento", vacios);
        resultado.put("resumen", resumen);
        resultado.put("diferencias", diferencias);
        resultado.put("avisos", conAviso);
        return resultado;
    }

    // ========================================================================
    // Factura de compra
    // ========================================================================

    /* (non-Javadoc)
     * @see com.saa.ejb.cxp.service.SustentoTributarioService#calcularSustento(java.lang.Long)
     */
    @Override
    public String calcularSustento(Long idFactura) throws Throwable {
        // 1) Excepcion: si el grupo con mayor base imponible de la factura tiene configurado
        // uno de los codigos de excepcion (activo fijo / inventario / reembolso), ese gana.
        // 2) Regla base (D-126, C37 2026-11-19): IVA de la factura + centro de costo de su
        // asiento. Antes de D-126 la regla base era solo el IVA (2026-08-27: 103 con IVA -> 01,
        // 28 sin IVA -> 02 en las 131 facturas de entonces; ver
        // docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md #6.2). Detalle en evaluar().
        ResultadoSustento r = evaluarSustento(FACTURA, idFactura);
        return r != null ? r.getSustento() : null;
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
        ResultadoSustento r = evaluarSustento(FACTURA, factura.getId());
        if (r == null || r.getSustento() == null) {
            return null;
        }
        factura.setSustentoTributario(r.getSustento());
        facturaCompraDaoService.save(factura, factura.getId());
        System.out.println("SustentoTributarioService: factura " + factura.getId()
                + " resuelta automaticamente con codSustento=" + r.getSustento() + " (" + r.getRegla() + ")");
        registrarAviso(r);
        return r.getSustento();
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
    // (D-126: IVA + centro de costo). Ver docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §9.
    // ========================================================================

    @Override
    public String calcularSustentoLiquidacion(Long idLiquidacion) throws Throwable {
        ResultadoSustento r = evaluarSustento(LIQUIDACION, idLiquidacion);
        return r != null ? r.getSustento() : null;
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
        ResultadoSustento r = evaluarSustento(LIQUIDACION, liquidacion.getId());
        if (r == null || r.getSustento() == null) {
            return null;
        }
        liquidacion.setSustentoTributario(r.getSustento());
        liquidacionCompraCompraDaoService.save(liquidacion, liquidacion.getId());
        System.out.println("SustentoTributarioService: liquidacion " + liquidacion.getId()
                + " resuelta automaticamente con codSustento=" + r.getSustento() + " (" + r.getRegla() + ")");
        registrarAviso(r);
        return r.getSustento();
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
        ResultadoSustento r = evaluarSustento(NOTA_CREDITO, idNotaCredito);
        return r != null ? r.getSustento() : null;
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
        ResultadoSustento r = evaluarSustento(NOTA_CREDITO, notaCredito.getId());
        if (r == null || r.getSustento() == null) {
            return null;
        }
        notaCredito.setSustentoTributario(r.getSustento());
        notaCreditoCompraDaoService.save(notaCredito, notaCredito.getId());
        System.out.println("SustentoTributarioService: nota de credito " + notaCredito.getId()
                + " resuelta automaticamente con codSustento=" + r.getSustento() + " (" + r.getRegla() + ")");
        registrarAviso(r);
        return r.getSustento();
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
        // base (D-126: IVA + centro de costo del asiento).
        ResultadoSustento r = evaluarSustento(NOTA_DEBITO, idNotaDebito);
        return r != null ? r.getSustento() : null;
    }

    @Override
    public String resolverSiFaltaNotaDebito(NotaDebitoCompra notaDebito) throws Throwable {
        if (notaDebito == null || notaDebito.getId() == null) {
            return null;
        }
        if (notaDebito.getSustentoTributario() != null) {
            return notaDebito.getSustentoTributario();
        }
        ResultadoSustento r = evaluarSustento(NOTA_DEBITO, notaDebito.getId());
        if (r == null || r.getSustento() == null) {
            return null;
        }
        notaDebito.setSustentoTributario(r.getSustento());
        notaDebitoCompraDaoService.save(notaDebito, notaDebito.getId());
        System.out.println("SustentoTributarioService: nota de debito " + notaDebito.getId()
                + " resuelta automaticamente con codSustento=" + r.getSustento() + " (" + r.getRegla() + ")");
        registrarAviso(r);
        return r.getSustento();
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
