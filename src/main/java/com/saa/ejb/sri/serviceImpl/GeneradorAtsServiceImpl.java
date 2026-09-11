package com.saa.ejb.sri.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.sri.service.GeneradorAtsService;
import com.saa.ejb.sri.service.dto.ResultadoGeneracionAts;
import com.saa.model.cxc.Establecimiento;
import com.saa.model.cxc.Facturador;
import com.saa.model.cxc.NotaCredito;
import com.saa.model.cxc.NotaDebito;
import com.saa.model.cxc.Factura;
import com.saa.model.cxp.DetalleRetencionCompraV2;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.model.cxp.RetencionCompraV2;
import com.saa.model.tsr.Titular;
import com.saa.rubros.Estado;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoIdentificacion;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

/**
 * Implementación de {@link GeneradorAtsService}. Ver el javadoc de la interfaz para el alcance
 * (qué genera y qué no) y docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §10 para el
 * contrato completo, incluida la limitación de {@code &lt;anulados&gt;} (no se puede distinguir
 * una anulación interna de una baja hecha en el portal del SRI — se documenta ahí, no aquí).
 *
 * <p><b>Estructura y nombres de elementos del XML del ATS</b> (raíz {@code &lt;iva&gt;},
 * {@code &lt;compras&gt;/&lt;detalleCompras&gt;}, {@code &lt;ventas&gt;/&lt;detalleVentas&gt;},
 * {@code &lt;anulados&gt;/&lt;detalleAnulados&gt;}) vienen del esquema público del ATS que el
 * SRI usa desde hace años — no cambian por instalación, a diferencia de las Tablas de catálogo
 * (2,4,5,11,13,14,20,21) que sí varían y de las que este documento solo tiene verificadas
 * 2,4,5,11,14 (§3.6). Nunca se validó contra el XSD oficial ni contra el validador del SRI: no
 * enviar un ATS real generado por este servicio sin esa prueba primero.</p>
 */
@Stateless
public class GeneradorAtsServiceImpl implements GeneradorAtsService {

    @PersistenceContext
    private EntityManager em;

    // ÍTEM 31 (2026-09-11): para derivar tipoCliente desde el rubro 35 (Tipo de Persona) cuando
    // tipoProveedorAts (Tabla 14) está vacío -- ver resolverTipoClienteVenta.
    @EJB
    private com.saa.basico.ejb.DetalleRubroService detalleRubroService;

    private static final int MAX_BYTES_ZIP = 8 * 1024 * 1024;

    /**
     * Tabla 4/5 del catálogo ATS (CATALOGO-ATS.md §4): sustentos válidos por tipo de
     * comprobante, solo los cuatro tipos de {@code &lt;compras&gt;} de esta empresa. Claves en
     * formato de 2 dígitos ("01","03","04","05"), igual que se graba {@code tipoComprobante} en
     * este sistema (ver {@code LiquidacionCompraServiceImpl.writeInfoTributaria}, que pasa "03"
     * literal) -- el catálogo los lista sin cero a la izquierda ("1","3","4","5"), es solo
     * formato de la tabla, no un código distinto.
     */
    private static final Map<String, Set<String>> SUSTENTOS_VALIDOS_POR_COMPROBANTE;
    static {
        Map<String, Set<String>> m = new LinkedHashMap<String, Set<String>>();
        Set<String> facturaYNotas = new java.util.HashSet<String>(Arrays.asList(
                "01", "02", "03", "04", "05", "06", "07", "08", "09", "14", "15", "00"));
        Set<String> liquidacion = new java.util.HashSet<String>(Arrays.asList(
                "01", "02", "03", "04", "05", "06", "07", "08", "14", "15"));
        m.put("01", facturaYNotas); // Factura
        m.put("03", liquidacion);   // Liquidación de compra
        m.put("04", facturaYNotas); // Nota de crédito
        m.put("05", facturaYNotas); // Nota de débito
        SUSTENTOS_VALIDOS_POR_COMPROBANTE = m;
    }

    @Override
    public ResultadoGeneracionAts generarAts(Long idFacturador, int anio, int mes) throws Throwable {
        System.out.println("=== generarAts | facturador=" + idFacturador + " | periodo=" + mes + "/" + anio + " ===");

        if (idFacturador == null) {
            throw new IncomeException("Debe indicar el facturador.");
        }
        if (mes < 1 || mes > 12) {
            throw new IncomeException("El mes debe estar entre 1 y 12.");
        }
        Facturador facturador = em.find(Facturador.class, idFacturador);
        if (facturador == null) {
            throw new IncomeException("No se encontró el facturador con ID: " + idFacturador);
        }
        if (facturador.getEmpresa() == null) {
            throw new IncomeException("El facturador " + idFacturador + " no tiene empresa asignada.");
        }
        Long idEmpresa = facturador.getEmpresa().getCodigo();

        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.atEndOfMonth();
        LocalDateTime desdeDT = desde.atStartOfDay();
        LocalDateTime hastaDT = hasta.atTime(23, 59, 59);

        List<String> avisos = new ArrayList<String>();

        // Tabla 1 (CATALOGO-ATS.md §2): un facturador RIMPE semestral declara el ATS por
        // SEMESTRE, no por mes -- Mes solo puede ser "06" o "12" para ese régimen. Este
        // generador siempre arma un período mensual; si el facturador es RIMPE, hay que
        // confirmar el régimen exacto (semestral vs. no) antes de usar el mes tal cual.
        if (Long.valueOf(1L).equals(facturador.getRimpe())
                || Long.valueOf(1L).equals(facturador.getPopularRimpe())) {
            avisos.add("El facturador está marcado como RIMPE (rimpe=" + facturador.getRimpe()
                    + ", popularRimpe=" + facturador.getPopularRimpe() + "). Si el régimen es RIMPE "
                    + "semestral, el ATS se declara por semestre, no por mes (Mes solo admite '06' o "
                    + "'12') — este generador siempre arma un período mensual; confirmar el régimen "
                    + "exacto con contabilidad antes de usar este ZIP para ese caso.");
        }

        List<LineaCompra> compras = new ArrayList<LineaCompra>();
        compras.addAll(comprasFacturaCompra(idEmpresa, desde, hasta, desdeDT, hastaDT, avisos));
        compras.addAll(comprasLiquidacion(idEmpresa, desde, hasta, desdeDT, hastaDT, avisos));
        compras.addAll(comprasNotaCredito(idEmpresa, desde, hasta, desdeDT, hastaDT, avisos));
        compras.addAll(comprasNotaDebito(idEmpresa, desde, hasta, desdeDT, hastaDT, avisos));

        List<LineaVenta> ventas = agruparVentas(idEmpresa, desdeDT, hastaDT, avisos);

        // ÍTEM 5 del encargo 2026-09-09: retenciones de compra (PGS.RCV2/DRC2), enlazadas por
        // autorización del documento. Se calcula ANTES de generarXml para que writeDetalleCompra
        // sólo tenga que buscar en el mapa. Ver DIAGNOSTICO-ATS-RECHAZADO-VALIDADOR.md §A.2/§A.3.
        java.util.Set<String> autorizacionesCompra = new java.util.HashSet<String>();
        for (LineaCompra c : compras) {
            if (c.autorizacion != null && !c.autorizacion.trim().isEmpty()) {
                autorizacionesCompra.add(c.autorizacion);
            }
        }
        Map<String, RetencionInfo> retencionesCompra = cargarRetencionesCompra(idEmpresa, autorizacionesCompra, avisos);

        // ÍTEM 8 del encargo 2026-09-09: <anulados> declara los secuenciales que EMITIMOS
        // NOSOTROS y anulamos -- nunca los de un documento que nos emitió un proveedor. Las 4
        // entidades de compra (FacturaCompra, LiquidacionCompraCompra, NotaCreditoCompra,
        // NotaDebitoCompra) NO van acá: eso es lo que hacía que AT082026 declarara 18 anulados con
        // 8 de ellos con <autorizacion></autorizacion> vacía (inválido). Sólo las 3 de venta.
        List<LineaAnulado> anulados = new ArrayList<LineaAnulado>();
        anulados.addAll(anuladosDe("Factura", "d.facturador.empresa.codigo", idEmpresa, desdeDT, hastaDT,
                "Factura", avisos));
        anulados.addAll(anuladosDe("NotaCredito", "d.facturador.empresa.codigo", idEmpresa, desdeDT, hastaDT,
                "Nota de crédito", avisos));
        anulados.addAll(anuladosDe("NotaDebito", "d.facturador.empresa.codigo", idEmpresa, desdeDT, hastaDT,
                "Nota de débito", avisos));
        if (!anulados.isEmpty()) {
            avisos.add("<anulados> incluye " + anulados.size() + " documento(s) anulados internamente "
                    + "en el sistema durante el período. NO se puede distinguir una anulación interna "
                    + "de una baja hecha por el portal del SRI en línea (el modelo no tiene ese dato, "
                    + "ver §10) — revisar la lista antes de enviar: si alguno ya fue dado de baja "
                    + "directamente en el portal del SRI, el SRI lo rechazará por duplicado.");
        }

        double totalVentasDeclarado = 0.0;
        for (LineaVenta v : ventas) {
            totalVentasDeclarado += v.baseGravada + v.base0 + v.baseNoObjeto;
        }

        String xml = generarXml(facturador, periodo, compras, ventas, anulados, totalVentasDeclarado,
                retencionesCompra, avisos);
        String nombreArchivoXml = String.format("AT%02d%04d.xml", mes, anio);
        String nombreArchivoZip = String.format("AT%02d%04d.zip", mes, anio);
        byte[] zip = empaquetar(nombreArchivoXml, xml);

        if (zip.length > MAX_BYTES_ZIP) {
            avisos.add("El ZIP generado pesa " + zip.length + " bytes, supera el máximo de "
                    + MAX_BYTES_ZIP + " (8 MB) que acepta el portal del SRI (§3.1). Este servicio "
                    + "no divide el período: hay que revisar el volumen antes de enviar.");
        }

        ResultadoGeneracionAts resultado = new ResultadoGeneracionAts();
        resultado.setNombreArchivo(nombreArchivoZip);
        resultado.setContenidoBase64(Base64.getEncoder().encodeToString(zip));
        resultado.setTamanoBytes(zip.length);
        resultado.setTotalCompras(compras.size());
        resultado.setTotalVentas(ventas.size());
        resultado.setTotalAnulados(anulados.size());
        resultado.setTotalVentasDeclarado(redondear(totalVentasDeclarado));
        resultado.setAvisos(avisos);

        System.out.println("✓ ATS generado: " + nombreArchivoZip + " | compras=" + compras.size()
                + " | ventas(agrupadas)=" + ventas.size() + " | anulados=" + anulados.size()
                + " | avisos=" + avisos.size());
        return resultado;
    }

    // =====================================================================
    // <compras> — una consulta por tabla, unificadas en LineaCompra
    // =====================================================================

    private List<LineaCompra> comprasFacturaCompra(Long idEmpresa, LocalDate desde, LocalDate hasta,
            LocalDateTime desdeDT, LocalDateTime hastaDT, List<String> avisos) {
        TypedQuery<FacturaCompra> q = em.createQuery(
                "select f from FacturaCompra f where f.empresa.codigo = :idEmpresa "
                        + "and f.estado = :activo and f.titular is not null "
                        + "and ((f.fechaRegistroContable between :desde and :hasta) "
                        + "or (f.fechaRegistroContable is null and f.fecha between :desdeDT and :hastaDT)) "
                        + "order by f.fecha", FacturaCompra.class);
        q.setParameter("idEmpresa", idEmpresa);
        q.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        q.setParameter("desdeDT", desdeDT);
        q.setParameter("hastaDT", hastaDT);
        List<LineaCompra> resultado = new ArrayList<LineaCompra>();
        for (FacturaCompra f : q.getResultList()) {
            resultado.add(new LineaCompra(f.getTipoComprobante(), f.getNumEstablecimiento(),
                    f.getNumPtoEmision(), f.getSecuencial(), f.getFecha() != null ? f.getFecha().toLocalDate() : null,
                    f.getAutorizacion(), f.getTitular(), f.getSustentoTributario(), f.getFechaRegistroContable(),
                    nvl(f.getSubtotal(), 0.0), nvl(f.getSubcero(), 0.0), nvl(f.getvIVA(), 0.0), nvl(f.getvICE(), 0.0),
                    formasPagoFacturaCompra(f.getId())));
            if (f.getSustentoTributario() == null) {
                avisos.add("Factura de compra " + f.getId() + " sin codSustento resuelto — no debería "
                        + "pasar (fase 2/6 lo resuelve siempre), revisar.");
            }
            if (f.getFechaRegistroContable() == null) {
                avisos.add("Factura de compra " + f.getId() + " sin fechaRegistro contable capturada: "
                        + "se usó la fecha de emisión como aproximación.");
            }
            validarSustentoContraComprobante(f.getTipoComprobante(), f.getSustentoTributario(),
                    f.getId(), "Factura de compra", avisos);
        }
        return resultado;
    }

    private List<LineaCompra> comprasLiquidacion(Long idEmpresa, LocalDate desde, LocalDate hasta,
            LocalDateTime desdeDT, LocalDateTime hastaDT, List<String> avisos) {
        TypedQuery<LiquidacionCompraCompra> q = em.createQuery(
                "select l from LiquidacionCompraCompra l where l.empresa.codigo = :idEmpresa "
                        + "and l.estado = :activo and l.titular is not null "
                        + "and ((l.fechaRegistroContable between :desde and :hasta) "
                        + "or (l.fechaRegistroContable is null and l.fecha between :desdeDT and :hastaDT)) "
                        + "order by l.fecha", LiquidacionCompraCompra.class);
        q.setParameter("idEmpresa", idEmpresa);
        q.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        q.setParameter("desdeDT", desdeDT);
        q.setParameter("hastaDT", hastaDT);
        List<LineaCompra> resultado = new ArrayList<LineaCompra>();
        for (LiquidacionCompraCompra l : q.getResultList()) {
            resultado.add(new LineaCompra(l.getTipoComprobante(), l.getNumEstablecimiento(),
                    l.getNumPtoEmision(), l.getSecuencial(), l.getFecha() != null ? l.getFecha().toLocalDate() : null,
                    l.getAutorizacion(), l.getTitular(), l.getSustentoTributario(), l.getFechaRegistroContable(),
                    nvl(l.getSubtotal(), 0.0), nvl(l.getSubcero(), 0.0), nvl(l.getvIVA(), 0.0), nvl(l.getvICE(), 0.0),
                    formasPagoLiquidacionCompra(l.getId())));
            if (l.getFechaRegistroContable() == null) {
                avisos.add("Liquidación de compra " + l.getId() + " sin fechaRegistro contable capturada: "
                        + "se usó la fecha de emisión como aproximación.");
            }
            validarSustentoContraComprobante(l.getTipoComprobante(), l.getSustentoTributario(),
                    l.getId(), "Liquidación de compra", avisos);
        }
        return resultado;
    }

    private List<LineaCompra> comprasNotaCredito(Long idEmpresa, LocalDate desde, LocalDate hasta,
            LocalDateTime desdeDT, LocalDateTime hastaDT, List<String> avisos) {
        TypedQuery<NotaCreditoCompra> q = em.createQuery(
                "select n from NotaCreditoCompra n where n.empresa.codigo = :idEmpresa "
                        + "and n.estado = :activo and n.titular is not null "
                        + "and ((n.fechaRegistroContable between :desde and :hasta) "
                        + "or (n.fechaRegistroContable is null and n.fecha between :desdeDT and :hastaDT)) "
                        + "order by n.fecha", NotaCreditoCompra.class);
        q.setParameter("idEmpresa", idEmpresa);
        q.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        q.setParameter("desdeDT", desdeDT);
        q.setParameter("hastaDT", hastaDT);
        List<LineaCompra> resultado = new ArrayList<LineaCompra>();
        for (NotaCreditoCompra n : q.getResultList()) {
            // ÍTEM 28 (2026-09-10): NotaCreditoCompra no tiene tabla de formas de pago propia
            // (verificado: no existe FormaPagoNotaCreditoCompra en com.saa.model.cxp) -- lista
            // vacía, cae al respaldo de DRC2 en writeDetalleCompra.
            resultado.add(new LineaCompra(n.getTipoComprobante(), n.getNumEstablecimiento(),
                    n.getNumPtoEmision(), n.getSecuencial(), n.getFecha() != null ? n.getFecha().toLocalDate() : null,
                    n.getAutorizacion(), n.getTitular(), n.getSustentoTributario(), n.getFechaRegistroContable(),
                    nvl(n.getSubtotal(), 0.0), nvl(n.getSubcero(), 0.0), nvl(n.getvIVA(), 0.0), nvl(n.getvICE(), 0.0),
                    java.util.Collections.<String>emptyList()));
            if (n.getFechaRegistroContable() == null) {
                avisos.add("Nota de crédito de compra " + n.getId() + " sin fechaRegistro contable "
                        + "capturada: se usó la fecha de emisión como aproximación.");
            }
            validarSustentoContraComprobante(n.getTipoComprobante(), n.getSustentoTributario(),
                    n.getId(), "Nota de crédito de compra", avisos);
        }
        return resultado;
    }

    private List<LineaCompra> comprasNotaDebito(Long idEmpresa, LocalDate desde, LocalDate hasta,
            LocalDateTime desdeDT, LocalDateTime hastaDT, List<String> avisos) {
        TypedQuery<NotaDebitoCompra> q = em.createQuery(
                "select n from NotaDebitoCompra n where n.empresa.codigo = :idEmpresa "
                        + "and n.estado = :activo and n.titular is not null "
                        + "and ((n.fechaRegistroContable between :desde and :hasta) "
                        + "or (n.fechaRegistroContable is null and n.fecha between :desdeDT and :hastaDT)) "
                        + "order by n.fecha", NotaDebitoCompra.class);
        q.setParameter("idEmpresa", idEmpresa);
        q.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        q.setParameter("desde", desde);
        q.setParameter("hasta", hasta);
        q.setParameter("desdeDT", desdeDT);
        q.setParameter("hastaDT", hastaDT);
        List<LineaCompra> resultado = new ArrayList<LineaCompra>();
        for (NotaDebitoCompra n : q.getResultList()) {
            // ÍTEM 28 (2026-09-10): NotaDebitoCompra tampoco tiene tabla de formas de pago propia
            // (verificado: no existe FormaPagoNotaDebitoCompra) -- lista vacía, cae al respaldo
            // de DRC2 en writeDetalleCompra.
            resultado.add(new LineaCompra(n.getTipoComprobante(), n.getNumEstablecimiento(),
                    n.getNumPtoEmision(), n.getSecuencial(), n.getFecha() != null ? n.getFecha().toLocalDate() : null,
                    n.getAutorizacion(), n.getTitular(), n.getSustentoTributario(), n.getFechaRegistroContable(),
                    nvl(n.getSubtotal(), 0.0), nvl(n.getSubcero(), 0.0), nvl(n.getvIVA(), 0.0), nvl(n.getvICE(), 0.0),
                    java.util.Collections.<String>emptyList()));
            if (n.getFechaRegistroContable() == null) {
                avisos.add("Nota de débito de compra " + n.getId() + " sin fechaRegistro contable "
                        + "capturada: se usó la fecha de emisión como aproximación.");
            }
            validarSustentoContraComprobante(n.getTipoComprobante(), n.getSustentoTributario(),
                    n.getId(), "Nota de débito de compra", avisos);
        }
        return resultado;
    }

    /**
     * ÍTEM 28 (2026-09-10), ERROR 1 del validador: "no ha reportado las FORMAS DE PAGO... aun
     * cuando la suma... excede USD 500". Hoy se sacaba de {@code DRC2.docResForPago} (la línea de
     * la RETENCIÓN), que es de origen equivocado -- la forma de pago es del documento de compra,
     * no de la retención que eventualmente le practicamos. La fuente real, medida contra el
     * código (no contra la sugerencia del encargo): {@code FacturaCompra.formaPago} (el campo
     * {@code Long} de cabecera, línea 132) <b>nunca se escribe</b> para documentos ingresados por
     * el proceso normal de carga SRI -- verificado con grep sobre {@code com.saa.ejb.cxp}: ningún
     * {@code facturaCompra.setFormaPago(...)}. Lo que SÍ se puebla, directo desde el XML del SRI
     * y ya en formato de 2 dígitos, es {@code FormaPagoFacturaCompra} (tabla {@code PGS.FPFM}) --
     * ver {@code ProcesoCargaDocumentosServiceImpl:1686-1696}, que arma una fila por cada
     * {@code <pago>} del XML ingresado. Se usa esa, no el campo Long de cabecera.
     */
    @SuppressWarnings("unchecked")
    private List<String> formasPagoFacturaCompra(Long idFacturaCompra) {
        List<String> codigos = em.createQuery(
                "select distinct fp.formaPago from FormaPagoFacturaCompra fp "
                        + "where fp.factura.id = :id and fp.formaPago is not null")
                .setParameter("id", idFacturaCompra)
                .getResultList();
        return codigos;
    }

    /**
     * Mismo criterio que {@link #formasPagoFacturaCompra}, para liquidación de compra: la tabla
     * dedicada es {@code FormaPagoLiquidacionCompraCompra} (PGS.FPLM), no un campo de cabecera --
     * verificado que {@code LiquidacionCompraCompra} tampoco tiene un campo {@code formaPago}
     * propio (grep sin resultados).
     */
    @SuppressWarnings("unchecked")
    private List<String> formasPagoLiquidacionCompra(Long idLiquidacion) {
        List<String> codigos = em.createQuery(
                "select distinct fp.formaPago from FormaPagoLiquidacionCompraCompra fp "
                        + "where fp.liquidacion.id = :id and fp.formaPago is not null")
                .setParameter("id", idLiquidacion)
                .getResultList();
        return codigos;
    }

    // =====================================================================
    // <ventas> — agrupada por (titular, tipoComprobante), §3.4
    // =====================================================================

    private List<LineaVenta> agruparVentas(Long idEmpresa, LocalDateTime desdeDT, LocalDateTime hastaDT,
            List<String> avisos) {
        Map<String, LineaVenta> agrupado = new LinkedHashMap<String, LineaVenta>();

        // ÍTEM 10 (encargo 2026-09-09, segunda tarea urgente): estas 3 son documentos de VENTA --
        // ver CriterioVentaVigente, "vigente" = autorizado y no anulado después. NO usar
        // Estado.ACTIVO acá.
        TypedQuery<Factura> qf = em.createQuery(
                "select f from Factura f where f.facturador.empresa.codigo = :idEmpresa "
                        + "and f.estado = :ventaAutorizada and f.estadoEmision <> :ventaNoAnulada "
                        + "and f.titular is not null "
                        + "and f.fecha between :desde and :hasta order by f.titular.codigo",
                Factura.class);
        qf.setParameter("idEmpresa", idEmpresa);
        qf.setParameter("ventaAutorizada", CriterioVentaVigente.ESTADO_AUTORIZADA);
        qf.setParameter("ventaNoAnulada", CriterioVentaVigente.ESTADO_EMISION_ANULADA);
        qf.setParameter("desde", desdeDT.toLocalDate());
        qf.setParameter("hasta", hastaDT.toLocalDate());
        for (Factura f : qf.getResultList()) {
            String tipoVenta = mapearTipoComprobanteVenta(f.getTipoComprobante(), f.getId(), "Factura", avisos);
            acumularVenta(agrupado, f.getTitular(), tipoVenta, f.getId(), nvl(f.getSubtotal(), 0.0),
                    nvl(f.getSubcero(), 0.0), nvl(f.getvIVA(), 0.0), nvl(f.getvICE(), 0.0));
        }

        TypedQuery<NotaCredito> qnc = em.createQuery(
                "select n from NotaCredito n where n.facturador.empresa.codigo = :idEmpresa "
                        + "and n.estado = :ventaAutorizada and n.estadoEmision <> :ventaNoAnulada "
                        + "and n.titular is not null "
                        + "and n.fecha between :desde and :hasta order by n.titular.codigo",
                NotaCredito.class);
        qnc.setParameter("idEmpresa", idEmpresa);
        qnc.setParameter("ventaAutorizada", CriterioVentaVigente.ESTADO_AUTORIZADA);
        qnc.setParameter("ventaNoAnulada", CriterioVentaVigente.ESTADO_EMISION_ANULADA);
        qnc.setParameter("desde", desdeDT);
        qnc.setParameter("hasta", hastaDT);
        for (NotaCredito n : qnc.getResultList()) {
            String tipoVenta = mapearTipoComprobanteVenta(n.getTipoComprobante(), n.getId(), "Nota de crédito", avisos);
            acumularVenta(agrupado, n.getTitular(), tipoVenta, n.getId(), nvl(n.getSubtotal(), 0.0),
                    nvl(n.getSubcero(), 0.0), nvl(n.getvIVA(), 0.0), nvl(n.getvICE(), 0.0));
        }

        TypedQuery<NotaDebito> qnd = em.createQuery(
                "select n from NotaDebito n where n.facturador.empresa.codigo = :idEmpresa "
                        + "and n.estado = :ventaAutorizada and n.estadoEmision <> :ventaNoAnulada "
                        + "and n.titular is not null "
                        + "and n.fecha between :desde and :hasta order by n.titular.codigo",
                NotaDebito.class);
        qnd.setParameter("idEmpresa", idEmpresa);
        qnd.setParameter("ventaAutorizada", CriterioVentaVigente.ESTADO_AUTORIZADA);
        qnd.setParameter("ventaNoAnulada", CriterioVentaVigente.ESTADO_EMISION_ANULADA);
        qnd.setParameter("desde", desdeDT);
        qnd.setParameter("hasta", hastaDT);
        for (NotaDebito n : qnd.getResultList()) {
            String tipoVenta = mapearTipoComprobanteVenta(n.getTipoComprobante(), n.getId(), "Nota de débito", avisos);
            acumularVenta(agrupado, n.getTitular(), tipoVenta, n.getId(), nvl(n.getSubtotal(), 0.0),
                    nvl(n.getSubcero(), 0.0), nvl(n.getvIVA(), 0.0), nvl(n.getvICE(), 0.0));
        }

        if (!agrupado.isEmpty()) {
            // ÍTEM 6 del encargo 2026-09-09: valorRetIva/valorRetRenta (la retención que el
            // cliente nos practicó) se emiten en 0.00 para TODAS las líneas -- no hay tabla en el
            // modelo que enlace una retención recibida a un documento de venta con su tipo de
            // impuesto (ver comentario en writeDetalleVenta). El archivo autorizado de julio no
            // era cero: 3 ventas con retención de IVA y 2 con retención de renta. Reportado al
            // árbitro, no inventado.
            avisos.add("<detalleVentas> declara valorRetIva=0.00 y valorRetRenta=0.00 en las " + agrupado.size()
                    + " línea(s) del período: no existe en el modelo una tabla que enlace la retención que "
                    + "el CLIENTE nos practica a un documento de venta con su tipo de impuesto (CBR.RTV2 es "
                    + "la retención que emitimos NOSOTROS a un proveedor, no sirve). El archivo autorizado "
                    + "de julio declaraba montos reales aquí (3 ventas con IVA, 2 con renta) -- revisar con "
                    + "el usuario si hace falta modelar esto antes de declarar, o si por ahora se acepta en "
                    + "0.00. No incluye tampoco la compensación (Tabla 21, sin verificar) — ver §10.");
        }
        return new ArrayList<LineaVenta>(agrupado.values());
    }

    private void acumularVenta(Map<String, LineaVenta> agrupado, Titular titular, String tipoComprobante,
            Long idDocumento, double baseGravada, double base0, double montoIva, double montoIce) {
        if (titular == null) {
            return;
        }
        String clave = titular.getCodigo() + "|" + tipoComprobante;
        LineaVenta linea = agrupado.get(clave);
        if (linea == null) {
            linea = new LineaVenta(titular, tipoComprobante);
            agrupado.put(clave, linea);
        }
        linea.numeroComprob++;
        linea.baseGravada += baseGravada;
        linea.base0 += base0;
        linea.montoIva += montoIva;
        linea.montoIce += montoIce;
        if (idDocumento != null) {
            linea.idsDocumento.add(idDocumento);
        }
    }

    // =====================================================================
    // <anulados> — ver limitación documentada en la clase y en §10
    // =====================================================================

    @SuppressWarnings("unchecked")
    private List<LineaAnulado> anuladosDe(String entidad, String campoEmpresa, Long idEmpresa,
            LocalDateTime desdeDT, LocalDateTime hastaDT, String etiqueta, List<String> avisos) {
        // ANULADA = 3 en todos los rubros de estadoEmision de este grupo de documentos —
        // mismo código que usa FacturaServiceImpl.anular / LiquidacionCompraServiceImpl, etc.
        // campoEmpresa difiere entre compra (empresa directa) y venta (via facturador.empresa).
        Query q = em.createQuery(
                "select d.tipoComprobante, d.numEstablecimiento, d.numPtoEmision, d.secuencial, "
                        + "d.autorizacion from " + entidad + " d where " + campoEmpresa + " = :idEmpresa "
                        + "and d.estadoEmision = 3 and d.fechaAnulacion between :desde and :hasta");
        q.setParameter("idEmpresa", idEmpresa);
        q.setParameter("desde", desdeDT);
        q.setParameter("hasta", hastaDT);
        List<LineaAnulado> resultado = new ArrayList<LineaAnulado>();
        for (Object[] fila : (List<Object[]>) q.getResultList()) {
            String autorizacion = (String) fila[4];
            // ÍTEM 8: un <detalleAnulados> con <autorizacion></autorizacion> vacía es inválido --
            // el AT082026 rechazado tenía 8 así. No se inventa el valor: se excluye y se avisa.
            if (autorizacion == null || autorizacion.trim().isEmpty()) {
                avisos.add(etiqueta + " " + fila[3] + " (secuencial) anulada en el período sin "
                        + "número de autorización -- excluida de <anulados> porque un elemento vacío "
                        + "ahí es inválido para el SRI. Revisar por qué no tiene autorización.");
                continue;
            }
            resultado.add(new LineaAnulado((String) fila[0], (String) fila[1], (String) fila[2],
                    (String) fila[3], autorizacion));
        }
        return resultado;
    }

    // =====================================================================
    // XML (StAX) — mismo patrón que LiquidacionCompraServiceImpl.writeElement
    // =====================================================================

    private String generarXml(Facturador facturador, YearMonth periodo, List<LineaCompra> compras,
            List<LineaVenta> ventas, List<LineaAnulado> anulados, double totalVentas,
            Map<String, RetencionInfo> retencionesCompra, List<String> avisos)
            throws Exception {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter w = factory.createXMLStreamWriter(sw);

        List<Establecimiento> establecimientos = establecimientosActivos(facturador.getId());

        w.writeStartElement("iva");
        w.writeCharacters("\n");

        writeElement(w, "TipoIDInformante", "R", 2);
        writeElement(w, "IdInformante", nvl(facturador.getNumDoc(), ""), 2);
        // ÍTEM 2 del encargo 2026-09-09: nombreComercial en vez de la razón social completa (148
        // caracteres, rechazado por el validador) -- DIAGNOSTICO-ATS-RECHAZADO-VALIDADOR.md §A.5.
        writeElement(w, "razonSocial", resolverRazonSocial(facturador, avisos), 2);
        writeElement(w, "Anio", String.valueOf(periodo.getYear()), 2);
        writeElement(w, "Mes", String.format("%02d", periodo.getMonthValue()), 2);
        writeElement(w, "numEstabRuc", String.format("%03d", establecimientos.size()), 2);
        writeElement(w, "totalVentas", formatDecimal(totalVentas), 2);
        writeElement(w, "codigoOperativo", "IVA", 2);
        // RegimenMicroempresa: SOLO RIMPE semestral (§3.2), y el modelo actual (Facturador.rimpe/
        // popularRimpe) no distingue semestral de otras variantes de RIMPE -- se omite siempre,
        // nunca "NO", tal como pide el propio campo cuando no aplica. Ver aviso en §10.

        w.writeCharacters("  ");
        w.writeStartElement("compras");
        w.writeCharacters("\n");
        for (LineaCompra c : compras) {
            writeDetalleCompra(w, c, retencionesCompra, avisos);
        }
        w.writeCharacters("  ");
        w.writeEndElement();
        w.writeCharacters("\n");

        w.writeCharacters("  ");
        w.writeStartElement("ventas");
        w.writeCharacters("\n");
        for (LineaVenta v : ventas) {
            writeDetalleVenta(w, v, avisos);
        }
        w.writeCharacters("  ");
        w.writeEndElement();
        w.writeCharacters("\n");

        // ÍTEM 7 del encargo 2026-09-09: sección entera que faltaba (0 ocurrencias antes de hoy).
        // Estructura exacta en DIAGNOSTICO-ATS-RECHAZADO-VALIDADOR.md §A.4. No hay venta
        // desagregada por establecimiento en el modelo -- con 1 solo establecimiento activo (caso
        // real de ASOPREP) esto es exacto; con más de uno se declara el total bajo el primero y se
        // avisa, en vez de inventar un reparto.
        w.writeCharacters("  ");
        w.writeStartElement("ventasEstablecimiento");
        w.writeCharacters("\n");
        if (establecimientos.isEmpty()) {
            avisos.add("El facturador " + facturador.getId() + " no tiene establecimientos activos: "
                    + "<ventasEstablecimiento> quedó vacío, revisar antes de enviar.");
        } else {
            if (establecimientos.size() > 1) {
                avisos.add("El facturador tiene " + establecimientos.size() + " establecimientos activos "
                        + "y no hay venta desagregada por establecimiento en el modelo -- se declaró el "
                        + "total de ventas del período bajo el primero (código "
                        + establecimientos.get(0).getCodigo() + "); revisar si corresponde repartirlo.");
            }
            for (int i = 0; i < establecimientos.size(); i++) {
                Establecimiento est = establecimientos.get(i);
                double ventasEst = (i == 0) ? totalVentas : 0.0;
                w.writeCharacters("    ");
                w.writeStartElement("ventaEst");
                w.writeCharacters("\n");
                writeElement(w, "codEstab", nvl(est.getCodigo(), ""), 6);
                writeElement(w, "ventasEstab", formatDecimal(ventasEst), 6);
                writeElement(w, "ivaComp", "0.00", 6);
                w.writeCharacters("    ");
                w.writeEndElement();
                w.writeCharacters("\n");
            }
        }
        w.writeCharacters("  ");
        w.writeEndElement();
        w.writeCharacters("\n");

        w.writeCharacters("  ");
        w.writeStartElement("anulados");
        w.writeCharacters("\n");
        for (LineaAnulado a : anulados) {
            writeDetalleAnulado(w, a);
        }
        w.writeCharacters("  ");
        w.writeEndElement();
        w.writeCharacters("\n");

        w.writeEndElement(); // iva
        w.writeEndDocument();
        w.close();
        return sw.toString();
    }

    private void writeDetalleCompra(XMLStreamWriter w, LineaCompra c, Map<String, RetencionInfo> retenciones,
            List<String> avisos) throws Exception {
        w.writeCharacters("    ");
        w.writeStartElement("detalleCompras");
        w.writeCharacters("\n");
        writeElement(w, "codSustento", nvl(c.codSustento, ""), 6);
        writeElement(w, "tpIdProv", tipoIdentificacionCompra(c.titular), 6);
        writeElement(w, "idProv", nvl(c.titular.getIdentificacion(), ""), 6);
        writeElement(w, "tipoComprobante", nvl(c.tipoComprobante, ""), 6);
        // Decisión del usuario 2026-09-09 (DIAGNOSTICO-ATS-RECHAZADO-VALIDADOR.md §A.5): NULL ->
        // "NO". Es una afirmación tributaria, no un default técnico -- el archivo autorizado de
        // julio declara "NO" en los 79 proveedores; se revierte en Titular.parteRelacionada en
        // cuanto contabilidad marque las excepciones. "tipoProv" y "denopr" NO van acá: no existen
        // en el esquema real (0 ocurrencias en las 79 compras del autorizado) y eran justo lo que
        // rompía la secuencia ante el validador ("se esperaba 'fechaRegistro'").
        writeElement(w, "parteRel", nvl(c.titular.getParteRelacionada(), "NO"), 6);
        writeElement(w, "fechaRegistro", formatFecha(c.fechaRegistro != null ? c.fechaRegistro : c.fechaEmision), 6);
        writeElement(w, "establecimiento", nvl(c.establecimiento, ""), 6);
        writeElement(w, "puntoEmision", nvl(c.puntoEmision, ""), 6);
        writeElement(w, "secuencial", nvl(c.secuencial, ""), 6);
        writeElement(w, "fechaEmision", formatFecha(c.fechaEmision), 6);
        writeElement(w, "autorizacion", nvl(c.autorizacion, ""), 6);
        // Sin columnas propias en el modelo actual para distinguir "no objeto de IVA" vs
        // "exento" del resto de la base 0%: baseNoGraIva y baseImpExe quedan en 0.00 -- ver
        // aviso general en §10, no se inventa el reparto.
        writeElement(w, "baseNoGraIva", "0.00", 6);
        writeElement(w, "baseImponible", formatDecimal(c.base0), 6);
        writeElement(w, "baseImpGrav", formatDecimal(c.baseGravada), 6);
        writeElement(w, "baseImpExe", "0.00", 6);
        writeElement(w, "montoIce", formatDecimal(c.montoIce), 6);
        writeElement(w, "montoIva", formatDecimal(c.montoIva), 6);

        // Retenciones de IVA/renta, pagoExterior y formasDePago -- DIAGNOSTICO-ATS-RECHAZADO-
        // VALIDADOR.md §A.2/§A.3. Sin retenciones.get(c.autorizacion) los 8 campos numéricos van
        // en 0.00 y NI air NI el bloque estabRetencion1..fechaEmiRet1 se escriben, tal como está
        // en el archivo autorizado para un documento sin retención.
        RetencionInfo ret = (c.autorizacion != null && !c.autorizacion.trim().isEmpty())
                ? retenciones.get(c.autorizacion) : null;
        writeElement(w, "valRetBien10", formatDecimal(ret != null ? ret.valRetBien10 : 0.0), 6);
        writeElement(w, "valRetServ20", formatDecimal(ret != null ? ret.valRetServ20 : 0.0), 6);
        writeElement(w, "valorRetBienes", formatDecimal(ret != null ? ret.valorRetBienes : 0.0), 6);
        writeElement(w, "valRetServ50", formatDecimal(ret != null ? ret.valRetServ50 : 0.0), 6);
        writeElement(w, "valorRetServicios", formatDecimal(ret != null ? ret.valorRetServicios : 0.0), 6);
        writeElement(w, "valRetServ100", formatDecimal(ret != null ? ret.valRetServ100 : 0.0), 6);
        // valorRetencionNc, totbasesImpReemb: 0.00 -- sin fuente hoy, igual que en el autorizado.
        writeElement(w, "valorRetencionNc", "0.00", 6);
        writeElement(w, "totbasesImpReemb", "0.00", 6);

        // Constante en las 79 compras del autorizado, con o sin retención.
        w.writeCharacters("      ");
        w.writeStartElement("pagoExterior");
        w.writeCharacters("\n");
        writeElement(w, "pagoLocExt", "01", 8);
        writeElement(w, "paisEfecPago", "NA", 8);
        writeElement(w, "aplicConvDobTrib", "NA", 8);
        writeElement(w, "pagExtSujRetNorLeg", "NA", 8);
        w.writeCharacters("      ");
        w.writeEndElement();
        w.writeCharacters("\n");

        // ÍTEM 28 (2026-09-10), ERROR 1: la forma de pago es del DOCUMENTO, no de la retención.
        // Primaria: c.formasPagoPrimario (FormaPagoFacturaCompra/FormaPagoLiquidacionCompraCompra,
        // ya en 2 dígitos). Respaldo: DRC2.docResForPago (la línea de la retención), sólo si la
        // primaria vino vacía -- NotaCreditoCompra/NotaDebitoCompra no tienen tabla propia, así
        // que para esas dos el respaldo es la única fuente, igual que antes.
        List<String> formasPago = !c.formasPagoPrimario.isEmpty()
                ? c.formasPagoPrimario
                : (ret != null && ret.formaPago != null
                        ? java.util.Collections.singletonList(ret.formaPago)
                        : java.util.Collections.<String>emptyList());
        // ÍTEM 30 (2026-09-11): UNA sola fórmula del umbral, usada por las dos ramas -- el ítem 28
        // la tenía duplicada en potencia (sólo en el "falta"), y el SRI reveló el reverso: exige
        // <formasDePago> cuando el documento supera USD 500 (confirmado, no de este mensaje --
        // medido en el .txt de Recibidos: SETEL 035949972, 124.71+18.71=143.42, misma fórmula) Y
        // se queja si aparece en un documento que NO supera. "> 500.00" estricto (el texto del SRI
        // dice "exceden"): un documento en exactamente 500.00 no la lleva hasta que se mida lo
        // contrario -- no se adivina el borde.
        double totalDocumento = c.base0 + c.baseGravada + c.montoIva + c.montoIce;
        boolean superaUmbral = totalDocumento > 500.0;
        if (superaUmbral && !formasPago.isEmpty()) {
            w.writeCharacters("      ");
            w.writeStartElement("formasDePago");
            w.writeCharacters("\n");
            for (String fp : formasPago) {
                writeElement(w, "formaPago", fp, 8);
            }
            w.writeCharacters("      ");
            w.writeEndElement();
            w.writeCharacters("\n");
        } else if (superaUmbral) {
            // No hay forma de pago de ningún origen, y el documento supera USD 500: el SRI lo
            // rechaza por esto exacto (medido: 15 compras > 500 sin formasDePago) -- no se deja
            // pasar mudo. Si NO supera el umbral, no se escribe el bloque Y no hace falta avisar
            // nada: es exactamente lo que corresponde (el SRI se queja si aparece de más).
            avisos.add("Compra " + nvl(c.tipoComprobante, "") + " " + nvl(c.establecimiento, "") + "-"
                    + nvl(c.puntoEmision, "") + "-" + nvl(c.secuencial, "") + " (autorización "
                    + nvl(c.autorizacion, "") + "), total " + formatDecimal(totalDocumento) + ": sin "
                    + "forma de pago en ningún origen (documento ni retención) y supera USD 500 -- "
                    + "el SRI va a rechazar el ATS por esto. Revisar el documento antes de declarar.");
        }

        if (ret != null && !ret.airLineas.isEmpty()) {
            w.writeCharacters("      ");
            w.writeStartElement("air");
            w.writeCharacters("\n");
            for (DetalleAir a : ret.airLineas) {
                w.writeCharacters("        ");
                w.writeStartElement("detalleAir");
                w.writeCharacters("\n");
                writeElement(w, "codRetAir", a.codRetAir, 10);
                writeElement(w, "baseImpAir", a.baseImpAir, 10);
                writeElement(w, "porcentajeAir", a.porcentajeAir, 10);
                writeElement(w, "valRetAir", a.valRetAir, 10);
                w.writeCharacters("        ");
                w.writeEndElement();
                w.writeCharacters("\n");
            }
            w.writeCharacters("      ");
            w.writeEndElement();
            w.writeCharacters("\n");
        }

        if (ret != null) {
            writeElement(w, "estabRetencion1", nvl(ret.estabRetencion1, ""), 6);
            writeElement(w, "ptoEmiRetencion1", nvl(ret.ptoEmiRetencion1, ""), 6);
            writeElement(w, "secRetencion1", nvl(ret.secRetencion1, ""), 6);
            writeElement(w, "autRetencion1", nvl(ret.autRetencion1, ""), 6);
            writeElement(w, "fechaEmiRet1", formatFecha(ret.fechaEmiRet1), 6);
        }

        w.writeCharacters("    ");
        w.writeEndElement();
        w.writeCharacters("\n");
    }

    private void writeDetalleVenta(XMLStreamWriter w, LineaVenta v, List<String> avisos) throws Exception {
        w.writeCharacters("    ");
        w.writeStartElement("detalleVentas");
        w.writeCharacters("\n");
        String tpIdCliente = tipoIdentificacionVenta(v.titular);
        writeElement(w, "tpIdCliente", tpIdCliente, 6);
        writeElement(w, "idCliente", nvl(v.titular.getIdentificacion(), ""), 6);
        // parteRelVtas (no "parteRel" -- nombre distinto del lado compras, DIAGNOSTICO-ATS-
        // RECHAZADO-VALIDADOR.md §3). Mismo default "NO" que en compras y por la misma decisión
        // del usuario (§A.5): NULL -> "NO", afirmación tributaria a revertir si contabilidad marca
        // excepciones. "denoCli" no va: no existe en el esquema real.
        writeElement(w, "parteRelVtas", nvl(v.titular.getParteRelacionada(), "NO"), 6);
        // ÍTEM 28 (2026-09-10), ERROR 3 -- corrección de un error del ítem 3: "tipoCliente" SÍ
        // existe en el esquema, condicional: sólo cuando tpIdCliente="06" (pasaporte). Julio no
        // tenía ningún cliente con pasaporte, así que el campo nunca se emitió en el archivo
        // autorizado -- su ausencia ahí no probaba que no existiera, sólo que no aplicaba ese mes.
        // Valor: Tabla 14 ("01"=Persona natural, "02"=Sociedad) -- ver resolverTipoClienteVenta
        // para de dónde sale (ítem 31, corrigió el ítem 28: no basta con tipoProveedorAts).
        if ("06".equals(tpIdCliente)) {
            String tipoCliente = resolverTipoClienteVenta(v.titular, avisos);
            if (tipoCliente != null) {
                writeElement(w, "tipoCliente", tipoCliente, 6);
            }
        }
        writeElement(w, "tipoComprobante", nvl(v.tipoComprobante, ""), 6);
        // tipoEmision (no "tipoEm"): las 19 ventas del archivo autorizado de julio son "F", no
        // "E" -- el comentario anterior que fijaba "E" como "confirmado para esta empresa" estaba
        // equivocado, corregido por decisión del usuario 2026-09-09 (§A.5).
        writeElement(w, "tipoEmision", "F", 6);
        writeElement(w, "numeroComprobantes", String.valueOf(v.numeroComprob), 6);
        writeElement(w, "baseNoGraIva", "0.00", 6);
        writeElement(w, "baseImponible", formatDecimal(v.base0), 6);
        writeElement(w, "baseImpGrav", formatDecimal(v.baseGravada), 6);
        writeElement(w, "montoIva", formatDecimal(v.montoIva), 6);
        writeElement(w, "montoIce", formatDecimal(v.montoIce), 6);
        // valorRetIva/valorRetRenta: retención que el CLIENTE nos practicó. CBR.RTV2 no sirve --
        // tiene FACTURADOR+PROVEEDOR, es la retención que EMITIMOS nosotros (verificado contra
        // RetencionV2.java). No se encontró ninguna tabla que enlace una retención recibida a un
        // documento de venta puntual con su tipo de impuesto (candidato revisado: TSR.CRTN /
        // CobroRetencion, ligada a Cobro+Plantilla, sin FK a Factura/NotaCredito/NotaDebito ni
        // columna de tipo de impuesto) -- ver ítem 6 del encargo, reportado al árbitro sin inventar
        // el valor.
        writeElement(w, "valorRetIva", "0.00", 6);
        writeElement(w, "valorRetRenta", "0.00", 6);

        // ÍTEM 28 (2026-09-10), ERROR 2: nunca se emitía. Fuente: FormaPagoFactura -- se prefiere
        // sobre Factura.getFormaPago() (Long, cabecera) porque FacturaServiceImpl garantiza que
        // SIEMPRE queda al menos una fila ahí tras emitir (si la lista venía vacía, sintetiza una
        // desde la cabecera y la persiste -- ver FacturaServiceImpl:463-480/1220-1245), así que es
        // la fuente que de verdad tiene dato, no la que podría estar en null igual. Sólo aplica a
        // facturas (tipoComprobante="18"): NotaCredito/NotaDebito de venta no tienen tabla de
        // formas de pago propia (verificado: no existe FormaPagoNotaCredito/FormaPagoNotaDebito).
        // Decisión documentada (no elegida en silencio): una línea de <detalleVentas> puede venir
        // de VARIAS facturas agrupadas por (titular, tipoComprobante) -- se emite la UNIÓN de los
        // códigos distintos de todas ellas, un <formaPago> por código, en vez de quedarse con uno
        // solo o repetir. Es la lectura más fiel: todas esas formas de pago se usaron de verdad en
        // el período para ese cliente.
        if ("18".equals(v.tipoComprobante) && !v.idsDocumento.isEmpty()) {
            List<String> formasPago = resolverFormasDePagoVenta(v.idsDocumento);
            if (!formasPago.isEmpty()) {
                w.writeCharacters("      ");
                w.writeStartElement("formasDePago");
                w.writeCharacters("\n");
                for (String fp : formasPago) {
                    writeElement(w, "formaPago", fp, 8);
                }
                w.writeCharacters("      ");
                w.writeEndElement();
                w.writeCharacters("\n");
            } else {
                double totalLinea = v.base0 + v.baseGravada + v.montoIva + v.montoIce;
                if (totalLinea > 500.0) {
                    avisos.add("Ventas de " + v.titular.getCodigo() + " (" + nvl(v.titular.getNombre(), "")
                            + "), tipoComprobante=" + v.tipoComprobante + ", total " + formatDecimal(totalLinea)
                            + ": sin forma de pago en ninguna de las " + v.idsDocumento.size()
                            + " factura(s) agrupadas, y supera USD 500 -- el SRI va a rechazar el ATS por "
                            + "esto. Revisar antes de declarar.");
                }
            }
        }

        w.writeCharacters("    ");
        w.writeEndElement();
        w.writeCharacters("\n");
    }

    /**
     * ÍTEM 31 (2026-09-11), corrige un diagnóstico propio del ítem 28. {@code Titular} tiene DOS
     * campos que parecen decir lo mismo ("tipo de persona") y NO son intercambiables -- medido
     * contra el caso real (titular AGHAYAR SEYIDOV, pasaporte, rechazado por el SRI):
     * <ul>
     * <li>{@code tipoProveedorAts} (TTLRTPAT, Tabla 14 ATS): la fuente que el ítem 28 leía sola.
     * Se ESCRIBE sólo si alguien la carga a mano por fuera de la pantalla -- verificado con grep
     * sobre TODO {@code saaFE}: cero apariciones, no está en ninguna pantalla. El usuario no
     * puede cargarla aunque quiera, y decirle "configure el dato" (como hacía el aviso del ítem
     * 28) era pedirle algo imposible.</li>
     * <li>{@code rubroTipoPersonaH} (TTLRRZZA, rubro 35 "Tipo de Persona"): SÍ está en la
     * pantalla de titulares y el usuario SÍ la carga -- AGHAYAR SEYIDOV la tiene en "NATURAL".
     * Pero ningún proceso de negocio la lee (ver el hallazgo aparte reportado al árbitro):
     * escrita y nunca consumida, mientras la otra se consume y nunca se escribe.</li>
     * </ul>
     * Por eso la resolución deriva del rubro 35 cuando la Tabla 14 está vacía, con el mismo
     * mecanismo por catálogo que ya usa {@code resolverTipoIdentificacionSujetoRetenido}
     * (RetencionV2ServiceImpl, ítem 13): P nulo no bloquea la lectura de H, cae a
     * {@link Rubros#TIPO_PERSONA} (35). <b>Si el catálogo devuelve un texto en vez de "01"/"02"
     * directo (ej. "NATURAL"/"JURIDICA"), NO se inventa el mapeo</b> -- es una decisión
     * tributaria, se avisa y se deja sin escribir el elemento.
     */
    private String resolverTipoClienteVenta(Titular titular, List<String> avisos) {
        String directo = titular.getTipoProveedorAts();
        if (directo != null && !directo.trim().isEmpty()) {
            return directo.trim();
        }
        if (titular.getRubroTipoPersonaH() != null) {
            // 1) Intento por catálogo (PDTRVLRV) -- gana si algún día se llena. Ver punto 2 más
            // abajo para por qué hoy nunca resuelve.
            try {
                long rubroP = titular.getRubroTipoPersonaP() != null
                        ? titular.getRubroTipoPersonaP().longValue() : Rubros.TIPO_PERSONA;
                String valorAlfa = detalleRubroService.selectValorStringByRubAltDetAlt(
                        (int) rubroP, titular.getRubroTipoPersonaH().intValue());
                if (valorAlfa != null && !valorAlfa.trim().isEmpty()) {
                    String candidato = valorAlfa.trim();
                    String normalizado = candidato.length() == 1 ? "0" + candidato : candidato;
                    if ("01".equals(normalizado) || "02".equals(normalizado)) {
                        return normalizado;
                    }
                    avisos.add("Titular " + titular.getCodigo() + " (" + nvl(titular.getNombre(), "")
                            + "): el catálogo del rubro 35 (Tipo de Persona) devolvió '" + candidato
                            + "', que no es '01' (natural) ni '02' (sociedad) directo -- no se derivó "
                            + "tipoCliente sin inventar el mapeo. Revisar el catálogo (PGS.LSRI/TSRI) "
                            + "antes de declarar.");
                    return null;
                }
            } catch (Throwable e) {
                System.err.println("⚠ No se pudo derivar tipoCliente desde el rubro 35 para el titular "
                        + titular.getCodigo() + ": " + e.getMessage());
            }

            // 2) ÍTEM 32 (2026-09-11). Medido por el usuario contra la base, no supuesto:
            //      select d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV
            //        from SCP.PDTR d join SCP.PRBR r on r.PRBRCDGO = d.PRBRCDGO
            //       where r.PRBRALTR = 35;
            //      1  NATURAL   (PDTRVLRV = NULL)
            //      2  JURIDICO  (PDTRVLRV = NULL)
            // El valor alfanumérico del rubro 35 está vacío en las DOS únicas filas que existen,
            // así que el intento por catálogo de arriba nunca va a resolver mientras siga así --
            // no es un caso raro, es el estado permanente de este rubro hoy. Se deriva del
            // ALTERNO directo. No es una correspondencia elegida a dedo: "NATURAL"/"JURIDICO" son
            // las dos únicas descripciones que tiene el rubro, y la Tabla 14 del ATS
            // (01=Persona natural, 02=Sociedad -- javadoc de Titular.tipoProveedorAts,
            // CATALOGO-ATS.md §9) es exactamente esa distinción: biunívoca contra las dos filas
            // medidas, no una elección.
            //
            // Es RESPALDO, no reemplazo: si algún día alguien llena PDTRVLRV con "01"/"02", el
            // intento por catálogo de arriba gana y este bloque no se alcanza. No se actualizó
            // SCP.PDTR hoy -- es catálogo compartido entre todos los equipos, y un UPDATE ahí no
            // se mete apurado el mismo día que el usuario tiene que declarar.
            long alterno = titular.getRubroTipoPersonaH().longValue();
            if (alterno == 1L) {
                return "01";
            }
            if (alterno == 2L) {
                return "02";
            }
            avisos.add("Titular " + titular.getCodigo() + " (" + nvl(titular.getNombre(), "")
                    + "): rubroTipoPersonaH=" + alterno + " no es 1 (NATURAL) ni 2 (JURIDICO) -- son "
                    + "las únicas dos filas que tiene el rubro 35 hoy (medido 2026-09-11). No se "
                    + "derivó tipoCliente sin inventar el mapeo para un alterno nuevo.");
            return null;
        }
        // ÍTEM 31 punto 3: el texto anterior pedía "configure el dato en el titular" -- imposible,
        // tipoProveedorAts no está en ninguna pantalla. El campo visible es el Tipo de Persona.
        avisos.add("Titular " + titular.getCodigo() + " (" + nvl(titular.getNombre(), "")
                + ") no tiene Tipo de Persona capturado (ni tipoProveedorAts ni el rubro 35) -- el "
                + "SRI exige 'tipoCliente' para tpIdCliente=06 (pasaporte) y no se pudo escribir. "
                + "Cargue el Tipo de Persona del titular (pantalla de Titulares) antes de declarar.");
        return null;
    }

    /**
     * Une las formas de pago (ya en 2 dígitos) de todas las facturas agrupadas en una línea de
     * {@code <detalleVentas>}, sin repetir código. Ver el comentario de {@link #writeDetalleVenta}
     * sobre por qué se usa {@code FormaPagoFactura} y no {@code Factura.getFormaPago()}.
     */
    @SuppressWarnings("unchecked")
    private List<String> resolverFormasDePagoVenta(List<Long> idsFactura) {
        List<String> codigos = em.createQuery(
                "select distinct fp.formaPago from FormaPagoFactura fp "
                        + "where fp.factura.id in :ids and fp.formaPago is not null")
                .setParameter("ids", idsFactura)
                .getResultList();
        return codigos;
    }

    private void writeDetalleAnulado(XMLStreamWriter w, LineaAnulado a) throws Exception {
        w.writeCharacters("    ");
        w.writeStartElement("detalleAnulados");
        w.writeCharacters("\n");
        writeElement(w, "tipoComprobante", nvl(a.tipoComprobante, ""), 6);
        writeElement(w, "establecimiento", nvl(a.establecimiento, ""), 6);
        writeElement(w, "puntoEmision", nvl(a.puntoEmision, ""), 6);
        // Un documento anulado, no un rango: inicio y fin son el mismo secuencial.
        writeElement(w, "secuencialInicio", nvl(a.secuencial, ""), 6);
        writeElement(w, "secuencialFin", nvl(a.secuencial, ""), 6);
        writeElement(w, "autorizacion", nvl(a.autorizacion, ""), 6);
        w.writeCharacters("    ");
        w.writeEndElement();
        w.writeCharacters("\n");
    }

    @SuppressWarnings("unchecked")
    private List<Establecimiento> establecimientosActivos(Long idFacturador) {
        return em.createQuery(
                "select e from Establecimiento e where e.facturador.id = :idFacturador and e.estado = :activo")
                .setParameter("idFacturador", idFacturador)
                .setParameter("activo", Long.valueOf(Estado.ACTIVO))
                .getResultList();
    }

    /**
     * ÍTEM 2 del encargo 2026-09-09. {@code nombreComercial} si no viene vacío; si viene vacío,
     * {@code razonSocial} truncada a 100 caracteres, con aviso siempre que se use este segundo
     * camino (DIAGNOSTICO-ATS-RECHAZADO-VALIDADOR.md §A.5 y §6.1.1).
     */
    private String resolverRazonSocial(Facturador facturador, List<String> avisos) {
        String comercial = facturador.getNombreComercial();
        if (comercial != null && !comercial.trim().isEmpty()) {
            return comercial.trim();
        }
        String razonSocial = nvl(facturador.getRazonSocial(), "");
        boolean seTrunca = razonSocial.length() > 100;
        String resultado = seTrunca ? razonSocial.substring(0, 100) : razonSocial;
        avisos.add("Facturador " + facturador.getId() + " sin NOMBRECOMERCIAL en la base: <razonSocial> "
                + "se llenó con la razón social" + (seTrunca ? " truncada a 100 caracteres" : "")
                + " ('" + resultado + "'). El validador del SRI rechazó la razón social completa (148 "
                + "caracteres) -- confirmar con el usuario si conviene cargar NOMBRECOMERCIAL.");
        return resultado;
    }

    /**
     * Tabla 20 del lado ventas (ÍTEM 4, DIAGNOSTICO-ATS-RECHAZADO-VALIDADOR.md §A.1.1): medido
     * contra el archivo autorizado, factura interno "01" viaja como "18"; nota de crédito "04" y
     * nota de débito "05" viajan igual. Cualquier otro valor se deja tal cual y se avisa -- no se
     * inventa un código nuevo.
     */
    private String mapearTipoComprobanteVenta(String tipoComprobante, Long idDocumento, String etiqueta,
            List<String> avisos) {
        if ("01".equals(tipoComprobante)) {
            return "18";
        }
        if ("04".equals(tipoComprobante) || "05".equals(tipoComprobante)) {
            return tipoComprobante;
        }
        avisos.add(etiqueta + " " + idDocumento + ": tipoComprobante '" + tipoComprobante + "' no está "
                + "verificado contra el archivo autorizado para <ventas> (sólo factura/nota de crédito/"
                + "nota de débito lo están) -- se dejó tal cual, revisar antes de enviar.");
        return tipoComprobante;
    }

    /**
     * ÍTEM 5 del encargo 2026-09-09. Carga las retenciones de compra (PGS.RCV2/DRC2) del período,
     * agrupadas por la autorización del documento sustento (DRC2.docResAutorizacion), y las
     * reparte según la Tabla 11 del anexo (DIAGNOSTICO-ATS-RECHAZADO-VALIDADOR.md §A.2/§A.3). Sólo
     * consulta las autorizaciones que de verdad aparecen en las compras del período -- no hace
     * selectAll() sobre PGS.DRC2.
     */
    private Map<String, RetencionInfo> cargarRetencionesCompra(Long idEmpresa,
            java.util.Set<String> autorizacionesCompra, List<String> avisos) {
        Map<String, RetencionInfo> resultado = new LinkedHashMap<String, RetencionInfo>();
        if (autorizacionesCompra.isEmpty()) {
            return resultado;
        }
        TypedQuery<DetalleRetencionCompraV2> q = em.createQuery(
                "select d from DetalleRetencionCompraV2 d where d.retencionCompraV2.empresa.codigo = :idEmpresa "
                        + "and d.estado = :activo and d.docResAutorizacion in :autorizaciones "
                        + "order by d.docResAutorizacion",
                DetalleRetencionCompraV2.class);
        q.setParameter("idEmpresa", idEmpresa);
        q.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        q.setParameter("autorizaciones", autorizacionesCompra);
        for (DetalleRetencionCompraV2 d : q.getResultList()) {
            String autorizacion = d.getDocResAutorizacion();
            RetencionInfo info = resultado.get(autorizacion);
            if (info == null) {
                info = new RetencionInfo();
                RetencionCompraV2 cabecera = d.getRetencionCompraV2();
                if (cabecera != null) {
                    info.estabRetencion1 = cabecera.getNumEstablecimiento();
                    info.ptoEmiRetencion1 = cabecera.getNumPtoEmision();
                    info.secRetencion1 = cabecera.getSecuencial();
                    info.autRetencion1 = cabecera.getAutorizacion();
                    info.fechaEmiRet1 = cabecera.getFecha() != null ? cabecera.getFecha().toLocalDate() : null;
                }
                resultado.put(autorizacion, info);
            }
            String codImpuesto = nvl(d.getCodImpuesto(), "");
            double valor = nvl(d.getValorReten(), 0.0);
            if ("2".equals(codImpuesto)) {
                // IVA -- Tabla 11 (§A.3): reparto por CODRETENCION, nunca a un campo elegido a dedo.
                String cod = nvl(d.getCodRetencion(), "");
                if ("9".equals(cod)) {
                    info.valRetBien10 += valor;
                } else if ("10".equals(cod)) {
                    info.valRetServ20 += valor;
                } else if ("1".equals(cod)) {
                    info.valorRetBienes += valor;
                } else if ("11".equals(cod)) {
                    info.valRetServ50 += valor;
                } else if ("2".equals(cod)) {
                    info.valorRetServicios += valor;
                } else if ("3".equals(cod)) {
                    info.valRetServ100 += valor;
                } else {
                    avisos.add("Retención de compra (autorización " + autorizacion + "): CODRETENCION '"
                            + cod + "' de IVA no está en la Tabla 11 del anexo -- no se asignó a ningún "
                            + "campo del ATS, revisar.");
                }
            } else if ("1".equals(codImpuesto)) {
                info.airLineas.add(new DetalleAir(nvl(d.getCodRetencion(), ""),
                        formatDecimal(nvl(d.getBaseImponible(), 0.0)),
                        formatDecimal(nvl(d.getPorcentajeReten(), 0.0)), formatDecimal(valor)));
            } else {
                avisos.add("Retención de compra (autorización " + autorizacion + "): CODIMPUESTO '"
                        + codImpuesto + "' no es '1' (renta) ni '2' (IVA) -- línea ignorada, revisar.");
            }
            if (info.formaPago == null) {
                String forma = d.getDocResForPago();
                if (forma != null && !forma.trim().isEmpty()) {
                    info.formaPago = forma.trim();
                }
            }
        }
        return resultado;
    }

    // =====================================================================
    // Empaquetado ZIP
    // =====================================================================

    private byte[] empaquetar(String nombreXml, String contenidoXml) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(nombreXml));
            zos.write(contenidoXml.getBytes("UTF-8"));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    // =====================================================================
    // Helpers — mismo criterio que LiquidacionCompraServiceImpl.writeElement/formatDecimal
    // =====================================================================

    private void writeElement(XMLStreamWriter writer, String name, String value, int indent) throws Exception {
        writer.writeCharacters("  ".repeat(indent / 2));
        writer.writeStartElement(name);
        writer.writeCharacters(value);
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    private String formatDecimal(double value) {
        // Locale.US explícito: con la JVM en es_EC "%.2f" imprime coma decimal y el SRI
        // rechaza el archivo -- mismo criterio que LiquidacionCompraServiceImpl.formatDecimal.
        return String.format(Locale.US, "%.2f", Math.abs(value));
    }

    private String formatFecha(LocalDate fecha) {
        if (fecha == null) {
            return "";
        }
        return String.format("%02d/%02d/%04d", fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
    }

    /**
     * Tabla 2 (CATALOGO-ATS.md §3): en compras, RUC=01, Cédula=02, Pasaporte=03 -- distinto del
     * numeral interno {@link TipoIdentificacion} (Cédula=1, RUC=2, Pasaporte=3, otro orden) y
     * distinto también del rango de venta (04-07). No es lo mismo que
     * {@code rubroTipoIdentificacionH} tal cual se emite hoy en el XML del comprobante
     * electrónico (ver {@code LiquidacionCompraServiceImpl.writeInfoTributaria}) -- ese XML es
     * otro esquema del SRI, con su propia codificación, y no se puede asumir que comparte
     * numeración con la Tabla 2 del ATS. Traduce el rubro interno; null/no reconocido -> "" (se
     * revisa en avisos, no se inventa un valor).
     */
    private String tipoIdentificacionCompra(Titular titular) {
        Long rubro = titular.getRubroTipoIdentificacionH();
        if (rubro == null) {
            return "";
        }
        if (rubro.intValue() == TipoIdentificacion.RUC) {
            return "01";
        }
        if (rubro.intValue() == TipoIdentificacion.CEDULA_IDENTIDAD) {
            return "02";
        }
        if (rubro.intValue() == TipoIdentificacion.PASAPORTE) {
            return "03";
        }
        return "";
    }

    /** Tabla 2, rango de venta: RUC=04, Cédula=05, Pasaporte=06. No hay forma de detectar
     *  "Consumidor final" (07) desde {@link TipoIdentificacion}, que no tiene ese valor. */
    private String tipoIdentificacionVenta(Titular titular) {
        Long rubro = titular.getRubroTipoIdentificacionH();
        if (rubro == null) {
            return "";
        }
        if (rubro.intValue() == TipoIdentificacion.RUC) {
            return "04";
        }
        if (rubro.intValue() == TipoIdentificacion.CEDULA_IDENTIDAD) {
            return "05";
        }
        if (rubro.intValue() == TipoIdentificacion.PASAPORTE) {
            return "06";
        }
        return "";
    }

    /**
     * Tabla 4 (CATALOGO-ATS.md §4): cada tipo de comprobante admite solo un subconjunto de
     * codSustento. Si el resuelto no encaja, no se corrige acá -- se avisa para revisión manual,
     * puede ser una excepción de grupo mal configurada o un tipoComprobante grabado distinto del
     * esperado para esa tabla (FCTC=1, LQCC=3, NTCC=4, NTDC=5).
     */
    private void validarSustentoContraComprobante(String tipoComprobante, String codSustento,
            Long idDocumento, String etiquetaDocumento, List<String> avisos) {
        if (tipoComprobante == null || codSustento == null) {
            return;
        }
        Set<String> validos = SUSTENTOS_VALIDOS_POR_COMPROBANTE.get(tipoComprobante);
        if (validos != null && !validos.contains(codSustento)) {
            avisos.add(etiquetaDocumento + " " + idDocumento + ": codSustento '" + codSustento
                    + "' no es válido para tipoComprobante '" + tipoComprobante + "' según la Tabla 4/5 "
                    + "del catálogo ATS (CATALOGO-ATS.md §4) -- revisar antes de enviar.");
        }
    }

    private String nvl(String value, String porDefecto) {
        return (value != null && !value.trim().isEmpty()) ? value : porDefecto;
    }

    private double nvl(Double value, double porDefecto) {
        return value != null ? value.doubleValue() : porDefecto;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    // =====================================================================
    // Estructuras internas — unifican los 4 tipos de compra / 3 de venta antes de escribir XML
    // =====================================================================

    private static class LineaCompra {
        final String tipoComprobante, establecimiento, puntoEmision, secuencial, autorizacion, codSustento;
        final LocalDate fechaEmision, fechaRegistro;
        final Titular titular;
        final double baseGravada, base0, montoIva, montoIce;
        /** ÍTEM 28 (2026-09-10): formas de pago DEL DOCUMENTO (no de la retención) -- vacía si el
         *  tipo de documento no tiene tabla propia (NotaCreditoCompra/NotaDebitoCompra). */
        final List<String> formasPagoPrimario;

        LineaCompra(String tipoComprobante, String establecimiento, String puntoEmision, String secuencial,
                LocalDate fechaEmision, String autorizacion, Titular titular, String codSustento,
                LocalDate fechaRegistro, double baseGravada, double base0, double montoIva, double montoIce,
                List<String> formasPagoPrimario) {
            this.tipoComprobante = tipoComprobante;
            this.establecimiento = establecimiento;
            this.puntoEmision = puntoEmision;
            this.secuencial = secuencial;
            this.fechaEmision = fechaEmision;
            this.autorizacion = autorizacion;
            this.titular = titular;
            this.codSustento = codSustento;
            this.fechaRegistro = fechaRegistro;
            this.baseGravada = baseGravada;
            this.base0 = base0;
            this.montoIva = montoIva;
            this.montoIce = montoIce;
            this.formasPagoPrimario = formasPagoPrimario;
        }
    }

    private static class LineaVenta {
        final Titular titular;
        final String tipoComprobante;
        int numeroComprob = 0;
        double baseGravada = 0.0, base0 = 0.0, baseNoObjeto = 0.0, montoIva = 0.0, montoIce = 0.0;
        /** ÍTEM 28 (2026-09-10): ids de los documentos agrupados en esta línea -- sólo se usan
         *  para resolver formasDePago cuando tipoComprobante="18" (factura); ver
         *  resolverFormasDePagoVenta(). NC/ND no tienen tabla de formas de pago propia. */
        final List<Long> idsDocumento = new ArrayList<Long>();

        LineaVenta(Titular titular, String tipoComprobante) {
            this.titular = titular;
            this.tipoComprobante = tipoComprobante;
        }
    }

    private static class LineaAnulado {
        final String tipoComprobante, establecimiento, puntoEmision, secuencial, autorizacion;

        LineaAnulado(String tipoComprobante, String establecimiento, String puntoEmision, String secuencial,
                String autorizacion) {
            this.tipoComprobante = tipoComprobante;
            this.establecimiento = establecimiento;
            this.puntoEmision = puntoEmision;
            this.secuencial = secuencial;
            this.autorizacion = autorizacion;
        }
    }

    /** Una línea de {@code air/detalleAir} en {@code <detalleCompras>} (ÍTEM 5, §A.2 del anexo). */
    private static class DetalleAir {
        final String codRetAir, baseImpAir, porcentajeAir, valRetAir;

        DetalleAir(String codRetAir, String baseImpAir, String porcentajeAir, String valRetAir) {
            this.codRetAir = codRetAir;
            this.baseImpAir = baseImpAir;
            this.porcentajeAir = porcentajeAir;
            this.valRetAir = valRetAir;
        }
    }

    /**
     * Retención de compra ya resuelta para un documento (clave: su autorización), lista para que
     * {@code writeDetalleCompra} la escriba sin volver a tocar {@code RCV2}/{@code DRC2}. Ver
     * {@code cargarRetencionesCompra} (ÍTEM 5 del encargo 2026-09-09).
     */
    private static class RetencionInfo {
        double valRetBien10 = 0.0, valRetServ20 = 0.0, valorRetBienes = 0.0, valRetServ50 = 0.0,
                valorRetServicios = 0.0, valRetServ100 = 0.0;
        List<DetalleAir> airLineas = new ArrayList<DetalleAir>();
        String formaPago;
        String estabRetencion1, ptoEmiRetencion1, secRetencion1, autRetencion1;
        LocalDate fechaEmiRet1;
    }
}
