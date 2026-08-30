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
import com.saa.model.cxc.Facturador;
import com.saa.model.cxc.NotaCredito;
import com.saa.model.cxc.NotaDebito;
import com.saa.model.cxc.Factura;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.model.tsr.Titular;
import com.saa.rubros.Estado;
import com.saa.rubros.TipoIdentificacion;

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

        // Actualizado 2026-08-28 (ítem 12): las 4 entidades de <compras> ya tienen fechaAnulacion
        // (motivoAnulacion/fechaAnulacion/usuarioAnulacion, mismo patrón que Factura/NotaCredito/
        // NotaDebito de venta) -- se conecta aquí. Antes de este cambio no había forma de filtrar
        // anulados de compra por período; ahora sí, exactamente igual que el lado venta.
        List<LineaAnulado> anulados = new ArrayList<LineaAnulado>();
        anulados.addAll(anuladosDe("FacturaCompra", "d.empresa.codigo", idEmpresa, desdeDT, hastaDT));
        anulados.addAll(anuladosDe("LiquidacionCompraCompra", "d.empresa.codigo", idEmpresa, desdeDT, hastaDT));
        anulados.addAll(anuladosDe("NotaCreditoCompra", "d.empresa.codigo", idEmpresa, desdeDT, hastaDT));
        anulados.addAll(anuladosDe("NotaDebitoCompra", "d.empresa.codigo", idEmpresa, desdeDT, hastaDT));
        anulados.addAll(anuladosDe("Factura", "d.facturador.empresa.codigo", idEmpresa, desdeDT, hastaDT));
        anulados.addAll(anuladosDe("NotaCredito", "d.facturador.empresa.codigo", idEmpresa, desdeDT, hastaDT));
        anulados.addAll(anuladosDe("NotaDebito", "d.facturador.empresa.codigo", idEmpresa, desdeDT, hastaDT));
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

        String xml = generarXml(facturador, periodo, compras, ventas, anulados, totalVentasDeclarado, avisos);
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
                    nvl(f.getSubtotal(), 0.0), nvl(f.getSubcero(), 0.0), nvl(f.getvIVA(), 0.0), nvl(f.getvICE(), 0.0)));
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
                    nvl(l.getSubtotal(), 0.0), nvl(l.getSubcero(), 0.0), nvl(l.getvIVA(), 0.0), nvl(l.getvICE(), 0.0)));
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
            resultado.add(new LineaCompra(n.getTipoComprobante(), n.getNumEstablecimiento(),
                    n.getNumPtoEmision(), n.getSecuencial(), n.getFecha() != null ? n.getFecha().toLocalDate() : null,
                    n.getAutorizacion(), n.getTitular(), n.getSustentoTributario(), n.getFechaRegistroContable(),
                    nvl(n.getSubtotal(), 0.0), nvl(n.getSubcero(), 0.0), nvl(n.getvIVA(), 0.0), nvl(n.getvICE(), 0.0)));
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
            resultado.add(new LineaCompra(n.getTipoComprobante(), n.getNumEstablecimiento(),
                    n.getNumPtoEmision(), n.getSecuencial(), n.getFecha() != null ? n.getFecha().toLocalDate() : null,
                    n.getAutorizacion(), n.getTitular(), n.getSustentoTributario(), n.getFechaRegistroContable(),
                    nvl(n.getSubtotal(), 0.0), nvl(n.getSubcero(), 0.0), nvl(n.getvIVA(), 0.0), nvl(n.getvICE(), 0.0)));
            if (n.getFechaRegistroContable() == null) {
                avisos.add("Nota de débito de compra " + n.getId() + " sin fechaRegistro contable "
                        + "capturada: se usó la fecha de emisión como aproximación.");
            }
            validarSustentoContraComprobante(n.getTipoComprobante(), n.getSustentoTributario(),
                    n.getId(), "Nota de débito de compra", avisos);
        }
        return resultado;
    }

    // =====================================================================
    // <ventas> — agrupada por (titular, tipoComprobante), §3.4
    // =====================================================================

    private List<LineaVenta> agruparVentas(Long idEmpresa, LocalDateTime desdeDT, LocalDateTime hastaDT,
            List<String> avisos) {
        Map<String, LineaVenta> agrupado = new LinkedHashMap<String, LineaVenta>();

        TypedQuery<Factura> qf = em.createQuery(
                "select f from Factura f where f.facturador.empresa.codigo = :idEmpresa "
                        + "and f.estado = :activo and f.titular is not null "
                        + "and f.fecha between :desde and :hasta order by f.titular.codigo",
                Factura.class);
        qf.setParameter("idEmpresa", idEmpresa);
        qf.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        qf.setParameter("desde", desdeDT.toLocalDate());
        qf.setParameter("hasta", hastaDT.toLocalDate());
        for (Factura f : qf.getResultList()) {
            acumularVenta(agrupado, f.getTitular(), f.getTipoComprobante(), nvl(f.getSubtotal(), 0.0),
                    nvl(f.getSubcero(), 0.0), nvl(f.getvIVA(), 0.0), nvl(f.getvICE(), 0.0));
        }

        TypedQuery<NotaCredito> qnc = em.createQuery(
                "select n from NotaCredito n where n.facturador.empresa.codigo = :idEmpresa "
                        + "and n.estado = :activo and n.titular is not null "
                        + "and n.fecha between :desde and :hasta order by n.titular.codigo",
                NotaCredito.class);
        qnc.setParameter("idEmpresa", idEmpresa);
        qnc.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        qnc.setParameter("desde", desdeDT);
        qnc.setParameter("hasta", hastaDT);
        for (NotaCredito n : qnc.getResultList()) {
            acumularVenta(agrupado, n.getTitular(), n.getTipoComprobante(), nvl(n.getSubtotal(), 0.0),
                    nvl(n.getSubcero(), 0.0), nvl(n.getvIVA(), 0.0), nvl(n.getvICE(), 0.0));
        }

        TypedQuery<NotaDebito> qnd = em.createQuery(
                "select n from NotaDebito n where n.facturador.empresa.codigo = :idEmpresa "
                        + "and n.estado = :activo and n.titular is not null "
                        + "and n.fecha between :desde and :hasta order by n.titular.codigo",
                NotaDebito.class);
        qnd.setParameter("idEmpresa", idEmpresa);
        qnd.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        qnd.setParameter("desde", desdeDT);
        qnd.setParameter("hasta", hastaDT);
        for (NotaDebito n : qnd.getResultList()) {
            acumularVenta(agrupado, n.getTitular(), n.getTipoComprobante(), nvl(n.getSubtotal(), 0.0),
                    nvl(n.getSubcero(), 0.0), nvl(n.getvIVA(), 0.0), nvl(n.getvICE(), 0.0));
        }

        if (!agrupado.isEmpty()) {
            avisos.add("<ventas> no incluye el bloque de retenciones que le practicaron al cliente "
                    + "(no está modelado por documento de venta) ni la compensación (Tabla 21, sin "
                    + "verificar) — ver §10.");
        }
        return new ArrayList<LineaVenta>(agrupado.values());
    }

    private void acumularVenta(Map<String, LineaVenta> agrupado, Titular titular, String tipoComprobante,
            double baseGravada, double base0, double montoIva, double montoIce) {
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
    }

    // =====================================================================
    // <anulados> — ver limitación documentada en la clase y en §10
    // =====================================================================

    @SuppressWarnings("unchecked")
    private List<LineaAnulado> anuladosDe(String entidad, String campoEmpresa, Long idEmpresa,
            LocalDateTime desdeDT, LocalDateTime hastaDT) {
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
            resultado.add(new LineaAnulado((String) fila[0], (String) fila[1], (String) fila[2],
                    (String) fila[3], (String) fila[4]));
        }
        return resultado;
    }

    // =====================================================================
    // XML (StAX) — mismo patrón que LiquidacionCompraServiceImpl.writeElement
    // =====================================================================

    private String generarXml(Facturador facturador, YearMonth periodo, List<LineaCompra> compras,
            List<LineaVenta> ventas, List<LineaAnulado> anulados, double totalVentas, List<String> avisos)
            throws Exception {
        StringWriter sw = new StringWriter();
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter w = factory.createXMLStreamWriter(sw);

        w.writeStartElement("iva");
        w.writeCharacters("\n");

        writeElement(w, "TipoIDInformante", "R", 2);
        writeElement(w, "IdInformante", nvl(facturador.getNumDoc(), ""), 2);
        writeElement(w, "razonSocial", nvl(facturador.getRazonSocial(), ""), 2);
        writeElement(w, "Anio", String.valueOf(periodo.getYear()), 2);
        writeElement(w, "Mes", String.format("%02d", periodo.getMonthValue()), 2);
        writeElement(w, "numEstabRuc", String.format("%03d", contarEstablecimientosActivos(facturador.getId())), 2);
        writeElement(w, "totalVentas", formatDecimal(totalVentas), 2);
        writeElement(w, "codigoOperativo", "IVA", 2);
        // RegimenMicroempresa: SOLO RIMPE semestral (§3.2), y el modelo actual (Facturador.rimpe/
        // popularRimpe) no distingue semestral de otras variantes de RIMPE -- se omite siempre,
        // nunca "NO", tal como pide el propio campo cuando no aplica. Ver aviso en §10.

        w.writeCharacters("  ");
        w.writeStartElement("compras");
        w.writeCharacters("\n");
        for (LineaCompra c : compras) {
            writeDetalleCompra(w, c);
        }
        w.writeCharacters("  ");
        w.writeEndElement();
        w.writeCharacters("\n");

        w.writeCharacters("  ");
        w.writeStartElement("ventas");
        w.writeCharacters("\n");
        for (LineaVenta v : ventas) {
            writeDetalleVenta(w, v);
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

    private void writeDetalleCompra(XMLStreamWriter w, LineaCompra c) throws Exception {
        w.writeCharacters("    ");
        w.writeStartElement("detalleCompras");
        w.writeCharacters("\n");
        writeElement(w, "codSustento", nvl(c.codSustento, ""), 6);
        writeElement(w, "tpIdProv", tipoIdentificacionCompra(c.titular), 6);
        writeElement(w, "idProv", nvl(c.titular.getIdentificacion(), ""), 6);
        writeElement(w, "tipoComprobante", nvl(c.tipoComprobante, ""), 6);
        writeElement(w, "parteRel", nvl(c.titular.getParteRelacionada(), ""), 6);
        writeElement(w, "tipoProv", nvl(c.titular.getTipoProveedorAts(), ""), 6);
        writeElement(w, "denopr", nvl(nvl(c.titular.getNombre(), c.titular.getRazonSocial()), ""), 6);
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
        // Retenciones de IVA/renta por documento, pago/exterior (Tabla 13), reembolsos
        // detallados, banano, dividendos: fuera de alcance de esta ronda, ver §10 -- no se
        // escriben (son opcionales cuando no aplican).
        w.writeCharacters("    ");
        w.writeEndElement();
        w.writeCharacters("\n");
    }

    private void writeDetalleVenta(XMLStreamWriter w, LineaVenta v) throws Exception {
        w.writeCharacters("    ");
        w.writeStartElement("detalleVentas");
        w.writeCharacters("\n");
        writeElement(w, "tpIdCliente", tipoIdentificacionVenta(v.titular), 6);
        writeElement(w, "idCliente", nvl(v.titular.getIdentificacion(), ""), 6);
        writeElement(w, "parteRel", nvl(v.titular.getParteRelacionada(), ""), 6);
        writeElement(w, "tipoCliente", nvl(v.titular.getTipoProveedorAts(), ""), 6);
        writeElement(w, "denoCli", nvl(nvl(v.titular.getNombre(), v.titular.getRazonSocial()), ""), 6);
        writeElement(w, "tipoComprobante", nvl(v.tipoComprobante, ""), 6);
        // Tabla 20 (CATALOGO-ATS.md §11): "E" facturación electrónica. Confirmado como default
        // para esta empresa -- toda su emisión es electrónica, no hay comprobantes físicos.
        writeElement(w, "tipoEm", "E", 6);
        writeElement(w, "numeroComprob", String.valueOf(v.numeroComprob), 6);
        writeElement(w, "baseNoGraIva", "0.00", 6);
        writeElement(w, "baseImponible", formatDecimal(v.base0), 6);
        writeElement(w, "baseImpGrav", formatDecimal(v.baseGravada), 6);
        writeElement(w, "montoIva", formatDecimal(v.montoIva), 6);
        writeElement(w, "montoIce", formatDecimal(v.montoIce), 6);
        // tipoEm (Tabla 20), tipoCompe/monto (Tabla 21), retenciones que le practicaron:
        // catálogos sin verificar o sin fuente por documento -- ver §10, no se escriben.
        w.writeCharacters("    ");
        w.writeEndElement();
        w.writeCharacters("\n");
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

    private int contarEstablecimientosActivos(Long idFacturador) {
        Long total = (Long) em.createQuery(
                "select count(e) from Establecimiento e where e.facturador.id = :idFacturador and e.estado = :activo")
                .setParameter("idFacturador", idFacturador)
                .setParameter("activo", Long.valueOf(Estado.ACTIVO))
                .getSingleResult();
        return total != null ? total.intValue() : 0;
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

        LineaCompra(String tipoComprobante, String establecimiento, String puntoEmision, String secuencial,
                LocalDate fechaEmision, String autorizacion, Titular titular, String codSustento,
                LocalDate fechaRegistro, double baseGravada, double base0, double montoIva, double montoIce) {
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
        }
    }

    private static class LineaVenta {
        final Titular titular;
        final String tipoComprobante;
        int numeroComprob = 0;
        double baseGravada = 0.0, base0 = 0.0, baseNoObjeto = 0.0, montoIva = 0.0, montoIce = 0.0;

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
}
