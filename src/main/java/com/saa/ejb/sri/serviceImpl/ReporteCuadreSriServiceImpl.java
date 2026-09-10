package com.saa.ejb.sri.serviceImpl;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.sri.service.ReporteCuadreSriService;
import com.saa.model.cxc.Facturador;
import com.saa.model.cxc.Factura;
import com.saa.model.cxc.NotaCredito;
import com.saa.model.cxc.NotaDebito;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.rubros.Estado;
import com.saa.rubros.SustentoTributarioSri;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

/**
 * Implementación de {@link ReporteCuadreSriService}. Ver el javadoc de la interfaz para el
 * alcance — cobertura parcial a propósito, ninguna casilla se calcula por suposición.
 */
@Stateless
public class ReporteCuadreSriServiceImpl implements ReporteCuadreSriService {

    @PersistenceContext
    private EntityManager em;

    /** Casillas del 103 (§2.1) cuyo código de retención coincide literalmente con el número
     *  de casilla, sin sufijo ni ambigüedad — únicos que se sugieren automáticamente. */
    private static final Set<String> CASILLAS_103_SIN_AMBIGUEDAD = new HashSet<String>(Arrays.asList(
            "302", "303", "304", "307", "308", "311", "314", "322", "323", "324", "325",
            "329", "330", "332", "340", "341", "342", "344"));

    @Override
    public Map<String, Object> calcularCuadre104(Long idFacturador, int anio, int mes) throws Throwable {
        System.out.println("=== calcularCuadre104 | facturador=" + idFacturador + " | periodo=" + mes + "/" + anio + " ===");
        Facturador facturador = validarFacturadorPeriodo(idFacturador, mes);
        Long idEmpresa = facturador.getEmpresa().getCodigo();
        LocalDateTime[] rango = rangoPeriodo(anio, mes);
        LocalDateTime desde = rango[0];
        LocalDateTime hasta = rango[1];

        List<Map<String, Object>> casillas = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> noDisponibles = new ArrayList<Map<String, Object>>();
        List<String> avisos = new ArrayList<String>();

        // ── Ventas: Factura (fecha LocalDate) + NotaCredito/NotaDebito venta (LocalDateTime) ──
        double[] ventas = sumarVentas(idEmpresa, desde, hasta);
        double vSubtotal = ventas[0], vSubtotal5 = ventas[1], vSubcero = ventas[2],
                vIva = ventas[3], vIva5 = ventas[4];

        agregarCasilla(casillas, "401/411/421", "Ventas locales gravadas tarifa ≠ 0% (bruto/neto/impuesto)",
                vSubtotal, vSubtotal, vIva);
        agregarCasilla(casillas, "425/435/445", "Ventas locales gravadas tarifa 5%",
                vSubtotal5, vSubtotal5, vIva5);
        agregarCasillaSimple(casillas, "403-406", "Ventas locales tarifa 0% — SIN clasificar entre "
                + "'da derecho a crédito' (405/406) y 'no da derecho' (403/404): el modelo no distingue "
                + "los dos casos, revisar a mano.", vSubcero);
        agregarCasilla(casillas, "409/419/429", "Total operaciones y ventas (bruto/neto/impuesto)",
                vSubtotal + vSubtotal5 + vSubcero, vSubtotal + vSubtotal5 + vSubcero, vIva + vIva5);

        // ── Compras: split por codSustento ya resuelto (crédito IVA vs costo/gasto) ──────────
        double[] compras = sumarCompras(idEmpresa, desde, hasta);
        double cConCredito = compras[0], cSinCredito = compras[1], cCero = compras[2],
                cIvaConCredito = compras[3];

        agregarCasillaSimple(casillas, "500/501", "Adquisiciones gravadas tarifa ≠ 0% CON derecho a "
                + "crédito tributario (codSustento 01/03/06)", cConCredito);
        agregarCasillaSimple(casillas, "502/512/522", "Otras adquisiciones gravadas tarifa ≠ 0% SIN "
                + "derecho a crédito (codSustento 02/04/07/08 y otros)", cSinCredito);
        agregarCasillaSimple(casillas, "507", "Adquisiciones gravadas tarifa 0%", cCero);

        // ── Resumen impositivo, simplificado (§1.3): sin ajustes ni compensaciones ───────────
        double impuestoCausado = redondear((vIva + vIva5) - cIvaConCredito);
        Map<String, Object> c601 = new LinkedHashMap<String, Object>();
        if (impuestoCausado >= 0) {
            c601.put("casilla", "601");
            c601.put("concepto", "Impuesto causado (ventas − compras con derecho a crédito; "
                    + "simplificado, sin ajustes ni saldo de meses anteriores)");
            c601.put("valor", redondear(impuestoCausado));
            casillas.add(c601);
        } else {
            Map<String, Object> c602 = new LinkedHashMap<String, Object>();
            c602.put("casilla", "602");
            c602.put("concepto", "Crédito tributario aplicable (compras con derecho a crédito "
                    + "superan el IVA de ventas; simplificado, sin ajustes)");
            c602.put("valor", redondear(-impuestoCausado));
            casillas.add(c602);
        }

        noDisponible(noDisponibles, "605/606/615/617", "Saldo crédito tributario del mes "
                + "anterior/próximo — requiere historial de declaraciones previas, no se rastrea.");
        noDisponible(noDisponibles, "609", "Retenciones de IVA que le han sido efectuadas — no hay "
                + "en el sistema una entidad de 'retenciones de IVA recibidas' distinta de RTV2/DRV2 "
                + "(que son las que la empresa EMITE a sus proveedores, no las que le retienen a ella).");
        noDisponible(noDisponibles, "621", "Retención de IVA por Petrocomercial/comercializadoras — "
                + "confirmado en §1.4 del levantamiento que no aplica a esta empresa; no calculado.");
        noDisponible(noDisponibles, "620", "Subtotal a pagar — depende de 605/606/609/621 y otros "
                + "ajustes no disponibles; no se calcula para no dar una cifra final engañosa.");

        // ── ÍTEM 24 (2026-09-10): Retenciones de IVA — agente de retención (721-731, 799) ────
        // La 609 (arriba) son retenciones que le HACEN a la empresa; éstas son las que la
        // empresa HACE a sus proveedores como agente de retención — mismo RTV2/DRV2 que ya usa
        // calcularCuadre103, mismo CriterioVentaVigente del ítem 10, mismo mapeo Tabla 11 que
        // GeneradorAtsServiceImpl.cargarRetencionesCompra (líneas ~810-822). Antes de este ítem
        // la sección no se calculaba NI se listaba en noDisponibles: el usuario no tenía forma
        // de saber que le faltaba declararla.
        double[] retIva = sumarRetencionesIvaEmitidas(idFacturador, desde, hasta, avisos);
        double r721 = retIva[0], r723 = retIva[1], r725 = retIva[2],
                r727 = retIva[3], r729 = retIva[4], r731 = retIva[5];
        // Las seis van aunque den 0.00: en el formulario real están las seis y se transcriben así.
        agregarCasillaSimple(casillas, "721", "Retenciones IVA — Retención del 10%", r721);
        agregarCasillaSimple(casillas, "723", "Retenciones IVA — Retención del 20%", r723);
        agregarCasillaSimple(casillas, "725", "Retenciones IVA — Retención del 30%", r725);
        agregarCasillaSimple(casillas, "727", "Retenciones IVA — Retención del 50%", r727);
        agregarCasillaSimple(casillas, "729", "Retenciones IVA — Retención del 70%", r729);
        agregarCasillaSimple(casillas, "731", "Retenciones IVA — Retención del 100%", r731);
        double totalImpuestoRetenido = redondear(r721 + r723 + r725 + r727 + r729 + r731);
        agregarCasillaSimple(casillas, "799",
                "TOTAL IMPUESTO RETENIDO (= 721+723+725+727+729+731)", totalImpuestoRetenido);
        noDisponible(noDisponibles, "801", "TOTAL IMPUESTO A PAGAR POR RETENCIÓN = (799-800-802). "
                + "800 (saldo de retenciones no cobradas de períodos anteriores) y 802 (retenciones de "
                + "este período no cobradas) no están en el sistema — no se inventan en 0.00. Si en la "
                + "práctica los dos son cero, 801 = 799 (" + totalImpuestoRetenido + " para este período).");

        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        resultado.put("idFacturador", idFacturador);
        resultado.put("periodo", String.format("%04d-%02d", anio, mes));
        resultado.put("casillas", casillas);
        resultado.put("noDisponibles", noDisponibles);
        resultado.put("avisos", avisos);
        return resultado;
    }

    @Override
    public Map<String, Object> calcularCuadre103(Long idFacturador, int anio, int mes) throws Throwable {
        System.out.println("=== calcularCuadre103 | facturador=" + idFacturador + " | periodo=" + mes + "/" + anio + " ===");
        validarFacturadorPeriodo(idFacturador, mes);
        LocalDateTime[] rango = rangoPeriodo(anio, mes);

        // ÍTEM 10 (encargo 2026-09-09): RetencionV2 es un documento que ASOPREP EMITE y somete al
        // SRI -- mismo flujo de emisión electrónica que Factura/NotaCredito/NotaDebito, ver
        // CriterioVentaVigente. "d.retencionV2.estado" (cabecera) usa el criterio nuevo;
        // "d.estado" (el detalle, DetalleRetencionV2) sigue siendo el flag genérico de siempre
        // (verificado: su javadoc dice "1=Activo, 0=Inactivo" y no tiene el patrón 3/4/5/6) --
        // ese no se toca.
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createQuery(
                "select d.codRetencion, sum(d.baseImponible), sum(d.valorReten) "
                        + "from DetalleRetencionV2 d "
                        + "where d.retencionV2.facturador.id = :idFacturador "
                        + "and d.retencionV2.estado = :ventaAutorizada "
                        + "and d.retencionV2.estadoEmision <> :ventaNoAnulada "
                        + "and d.estado = :activo "
                        + "and d.retencionV2.fecha between :desde and :hasta "
                        + "group by d.codRetencion order by d.codRetencion")
                .setParameter("idFacturador", idFacturador)
                .setParameter("ventaAutorizada", CriterioVentaVigente.ESTADO_AUTORIZADA)
                .setParameter("ventaNoAnulada", CriterioVentaVigente.ESTADO_EMISION_ANULADA)
                .setParameter("activo", Long.valueOf(Estado.ACTIVO))
                .setParameter("desde", rango[0])
                .setParameter("hasta", rango[1])
                .getResultList();

        List<Map<String, Object>> porCodigo = new ArrayList<Map<String, Object>>();
        for (Object[] fila : filas) {
            String codigo = (String) fila[0];
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("codRetencion", codigo);
            item.put("casillaSugerida", CASILLAS_103_SIN_AMBIGUEDAD.contains(codigo) ? codigo : null);
            item.put("baseImponible", redondear(((Number) fila[1]).doubleValue()));
            item.put("valorRetenido", redondear(((Number) fila[2]).doubleValue()));
            porCodigo.add(item);
        }

        List<String> avisos = new ArrayList<String>();
        avisos.add("El mapeo código de retención → casilla del 103 NO está confirmado contra una "
                + "declaración real (advertencia explícita del levantamiento, §0 y §2.1): la "
                + "resolución NAC-DGERCGC26-00000009 (vigente desde 1-mar-2026) cambió porcentajes "
                + "y agregó el código 303A (5%, servicios profesionales de sociedades) que el "
                + "instructivo oficial —desactualizado— no contempla. 'casillaSugerida' solo se llena "
                + "cuando el código coincide literalmente con un número de casilla del §2.1, sin "
                + "sufijo (303, 304, 312, 320, etc.); códigos como 303A/304B/3440 quedan sin sugerir "
                + "a propósito — no se inventa a qué casilla exacta corresponden.");
        avisos.add("Solo cubre retenciones EMITIDAS a proveedores (RTV2/DRV2, 'pagos en el país' del "
                + "§2.1). No incluye pagos al exterior (§2.2) — sin retenciones de ese tipo verificadas "
                + "en el modelo para esta ronda.");

        Map<String, Object> resultado = new LinkedHashMap<String, Object>();
        resultado.put("idFacturador", idFacturador);
        resultado.put("periodo", String.format("%04d-%02d", anio, mes));
        resultado.put("porCodigo", porCodigo);
        resultado.put("avisos", avisos);
        return resultado;
    }

    // =====================================================================
    // Sumas por origen
    // =====================================================================

    /** {baseGeneral, base5, base0, ivaGeneral, iva5} de Factura + NotaCredito + NotaDebito de venta. */
    private double[] sumarVentas(Long idEmpresa, LocalDateTime desde, LocalDateTime hasta) {
        double[] totales = new double[5];

        // ÍTEM 10 (encargo 2026-09-09): las 3 de venta usan CriterioVentaVigente, no Estado.ACTIVO.
        TypedQuery<Factura> qf = em.createQuery(
                "select f from Factura f where f.facturador.empresa.codigo = :idEmpresa "
                        + "and f.estado = :ventaAutorizada and f.estadoEmision <> :ventaNoAnulada "
                        + "and f.fecha between :desde and :hasta",
                Factura.class);
        qf.setParameter("idEmpresa", idEmpresa);
        qf.setParameter("ventaAutorizada", CriterioVentaVigente.ESTADO_AUTORIZADA);
        qf.setParameter("ventaNoAnulada", CriterioVentaVigente.ESTADO_EMISION_ANULADA);
        qf.setParameter("desde", desde.toLocalDate());
        qf.setParameter("hasta", hasta.toLocalDate());
        for (Factura f : qf.getResultList()) {
            totales[0] += nvl(f.getSubtotal());
            totales[1] += nvl(f.getSubtotal5());
            totales[2] += nvl(f.getSubcero());
            totales[3] += nvl(f.getvIVA());
            totales[4] += nvl(f.getvIVA5());
        }

        TypedQuery<NotaCredito> qnc = em.createQuery(
                "select n from NotaCredito n where n.facturador.empresa.codigo = :idEmpresa "
                        + "and n.estado = :ventaAutorizada and n.estadoEmision <> :ventaNoAnulada "
                        + "and n.fecha between :desde and :hasta",
                NotaCredito.class);
        qnc.setParameter("idEmpresa", idEmpresa);
        qnc.setParameter("ventaAutorizada", CriterioVentaVigente.ESTADO_AUTORIZADA);
        qnc.setParameter("ventaNoAnulada", CriterioVentaVigente.ESTADO_EMISION_ANULADA);
        qnc.setParameter("desde", desde);
        qnc.setParameter("hasta", hasta);
        for (NotaCredito n : qnc.getResultList()) {
            // Notas de crédito RESTAN de la venta (§1.1: se netean contra la venta del mes).
            totales[0] -= nvl(n.getSubtotal());
            totales[2] -= nvl(n.getSubcero());
            totales[3] -= nvl(n.getvIVA());
        }

        TypedQuery<NotaDebito> qnd = em.createQuery(
                "select n from NotaDebito n where n.facturador.empresa.codigo = :idEmpresa "
                        + "and n.estado = :ventaAutorizada and n.estadoEmision <> :ventaNoAnulada "
                        + "and n.fecha between :desde and :hasta",
                NotaDebito.class);
        qnd.setParameter("idEmpresa", idEmpresa);
        qnd.setParameter("ventaAutorizada", CriterioVentaVigente.ESTADO_AUTORIZADA);
        qnd.setParameter("ventaNoAnulada", CriterioVentaVigente.ESTADO_EMISION_ANULADA);
        qnd.setParameter("desde", desde);
        qnd.setParameter("hasta", hasta);
        for (NotaDebito n : qnd.getResultList()) {
            totales[0] += nvl(n.getSubtotal());
            totales[2] += nvl(n.getSubcero());
            totales[3] += nvl(n.getvIVA());
        }

        return totales;
    }

    /** {baseConCredito, baseSinCredito, base0, ivaConCredito} de FCTC+LQCC+NTCC+NTDC, por codSustento. */
    private double[] sumarCompras(Long idEmpresa, LocalDateTime desde, LocalDateTime hasta) {
        double[] totales = new double[4];
        List<String> codigosConCredito = Arrays.asList(
                SustentoTributarioSri.CREDITO_TRIBUTARIO_IVA,
                SustentoTributarioSri.ACTIVO_FIJO_CREDITO_IVA,
                SustentoTributarioSri.INVENTARIO_CREDITO_IVA);

        TypedQuery<FacturaCompra> qf = em.createQuery(
                "select f from FacturaCompra f where f.empresa.codigo = :idEmpresa "
                        + "and f.estado = :activo and f.fecha between :desde and :hasta", FacturaCompra.class);
        qf.setParameter("idEmpresa", idEmpresa);
        qf.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        qf.setParameter("desde", desde);
        qf.setParameter("hasta", hasta);
        for (FacturaCompra f : qf.getResultList()) {
            acumularCompra(totales, codigosConCredito, f.getSustentoTributario(), nvl(f.getSubtotal()),
                    nvl(f.getSubcero()), nvl(f.getvIVA()));
        }

        TypedQuery<LiquidacionCompraCompra> ql = em.createQuery(
                "select l from LiquidacionCompraCompra l where l.empresa.codigo = :idEmpresa "
                        + "and l.estado = :activo and l.fecha between :desde and :hasta", LiquidacionCompraCompra.class);
        ql.setParameter("idEmpresa", idEmpresa);
        ql.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        ql.setParameter("desde", desde);
        ql.setParameter("hasta", hasta);
        for (LiquidacionCompraCompra l : ql.getResultList()) {
            acumularCompra(totales, codigosConCredito, l.getSustentoTributario(), nvl(l.getSubtotal()),
                    nvl(l.getSubcero()), nvl(l.getvIVA()));
        }

        TypedQuery<NotaCreditoCompra> qnc = em.createQuery(
                "select n from NotaCreditoCompra n where n.empresa.codigo = :idEmpresa "
                        + "and n.estado = :activo and n.fecha between :desde and :hasta", NotaCreditoCompra.class);
        qnc.setParameter("idEmpresa", idEmpresa);
        qnc.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        qnc.setParameter("desde", desde);
        qnc.setParameter("hasta", hasta);
        for (NotaCreditoCompra n : qnc.getResultList()) {
            // Nota de crédito de compra: resta (se recibió a favor).
            acumularCompra(totales, codigosConCredito, n.getSustentoTributario(), -nvl(n.getSubtotal()),
                    -nvl(n.getSubcero()), -nvl(n.getvIVA()));
        }

        TypedQuery<NotaDebitoCompra> qnd = em.createQuery(
                "select n from NotaDebitoCompra n where n.empresa.codigo = :idEmpresa "
                        + "and n.estado = :activo and n.fecha between :desde and :hasta", NotaDebitoCompra.class);
        qnd.setParameter("idEmpresa", idEmpresa);
        qnd.setParameter("activo", Long.valueOf(Estado.ACTIVO));
        qnd.setParameter("desde", desde);
        qnd.setParameter("hasta", hasta);
        for (NotaDebitoCompra n : qnd.getResultList()) {
            acumularCompra(totales, codigosConCredito, n.getSustentoTributario(), nvl(n.getSubtotal()),
                    nvl(n.getSubcero()), nvl(n.getvIVA()));
        }

        return totales;
    }

    private void acumularCompra(double[] totales, List<String> codigosConCredito, String sustento,
            double base, double base0, double iva) {
        if (sustento != null && codigosConCredito.contains(sustento)) {
            totales[0] += base;
            totales[3] += iva;
        } else {
            totales[1] += base;
        }
        totales[2] += base0;
    }

    /**
     * ÍTEM 24 (2026-09-10). {valRetBien10 (721), valRetServ20 (723), valorRetBienes (725),
     * valRetServ50 (727), valorRetServicios (729), valRetServ100 (731)}: retenciones de IVA
     * (codImpuesto=2) que la empresa practica a sus proveedores como agente de retención.
     * <p>
     * Fuente: {@code DetalleRetencionV2} + {@code RetencionV2} (CBR.DRV2/RTV2) -- la misma que
     * ya usa {@code calcularCuadre103}, y determinada por lo que representa cada tabla, no sólo
     * por consistencia con el 103: {@code RetencionV2ServiceImpl} (este paquete, cxc) es el
     * servicio completo de emisión electrónica -- genera el XML v2.0.0, lo firma, lo somete al
     * SRI por SOAP y sigue su estado 1-6 (ver {@code CriterioVentaVigente}); en cambio
     * {@code RetencionCompraV2ServiceImpl} (cxp, PGS.RCV2/DRC2, usado en el bloque
     * {@code <air>} del ATS) es un CRUD genérico de 45 líneas sin ninguna lógica de emisión ni
     * de estado. Sólo CBR.RTV2/DRV2 representa comprobantes de retención efectivamente emitidos
     * y autorizados por el SRI, que es lo único que el formulario 104 puede declarar.
     * <p>
     * Mismos filtros que {@code calcularCuadre103}: facturador, rango de fecha de la RETENCIÓN
     * (no del documento sustento), y {@code CriterioVentaVigente} (autorizada y no anulada). A
     * diferencia del 103, acá SÍ se filtra {@code codImpuesto='2'} explícitamente -- el 103 no
     * lo filtra porque agrupa por {@code codRetencion} sin distinguir impuesto, cosa que no se
     * tocó (fuera del alcance de este ítem) pero que conviene que quede escrita: los códigos de
     * IVA (1,2,3,9,10,11) y de renta (303, 304, 312, 320, 3440...) no colisionan numéricamente
     * en la práctica, pero la ausencia del filtro en el 103 es una omisión, no una decisión.
     * <p>
     * Mapeo Tabla 11 → casilla: idéntico al que ya usa
     * {@code GeneradorAtsServiceImpl.cargarRetencionesCompra} (líneas ~810-822) -- reusado, no
     * reescrito de memoria. Un {@code codRetencion} fuera de la tabla no se asigna a ninguna
     * casilla: va a {@code avisos}.
     */
    private double[] sumarRetencionesIvaEmitidas(Long idFacturador, LocalDateTime desde, LocalDateTime hasta,
            List<String> avisos) {
        double[] totales = new double[6]; // 721, 723, 725, 727, 729, 731
        @SuppressWarnings("unchecked")
        List<Object[]> filas = em.createQuery(
                "select d.codRetencion, sum(d.valorReten) "
                        + "from DetalleRetencionV2 d "
                        + "where d.retencionV2.facturador.id = :idFacturador "
                        + "and d.retencionV2.estado = :ventaAutorizada "
                        + "and d.retencionV2.estadoEmision <> :ventaNoAnulada "
                        + "and d.estado = :activo "
                        + "and d.codImpuesto = :iva "
                        + "and d.retencionV2.fecha between :desde and :hasta "
                        + "group by d.codRetencion")
                .setParameter("idFacturador", idFacturador)
                .setParameter("ventaAutorizada", CriterioVentaVigente.ESTADO_AUTORIZADA)
                .setParameter("ventaNoAnulada", CriterioVentaVigente.ESTADO_EMISION_ANULADA)
                .setParameter("activo", Long.valueOf(Estado.ACTIVO))
                .setParameter("iva", "2")
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
        for (Object[] fila : filas) {
            String cod = (String) fila[0];
            double valor = fila[1] != null ? ((Number) fila[1]).doubleValue() : 0.0;
            // Tabla 11, idéntica a GeneradorAtsServiceImpl.cargarRetencionesCompra.
            if ("9".equals(cod)) {
                totales[0] += valor;
            } else if ("10".equals(cod)) {
                totales[1] += valor;
            } else if ("1".equals(cod)) {
                totales[2] += valor;
            } else if ("11".equals(cod)) {
                totales[3] += valor;
            } else if ("2".equals(cod)) {
                totales[4] += valor;
            } else if ("3".equals(cod)) {
                totales[5] += valor;
            } else {
                avisos.add("Retención de IVA emitida con CODRETENCION '" + cod + "' no está en la "
                        + "Tabla 11 del formulario 104 (casillas 721-731) -- no se asignó a ninguna "
                        + "casilla, revisar.");
            }
        }
        return totales;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Facturador validarFacturadorPeriodo(Long idFacturador, int mes) throws Throwable {
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
        return facturador;
    }

    private LocalDateTime[] rangoPeriodo(int anio, int mes) {
        YearMonth periodo = YearMonth.of(anio, mes);
        LocalDateTime desde = periodo.atDay(1).atStartOfDay();
        LocalDateTime hasta = periodo.atEndOfMonth().atTime(23, 59, 59);
        return new LocalDateTime[] { desde, hasta };
    }

    private void agregarCasilla(List<Map<String, Object>> casillas, String casilla, String concepto,
            double bruto, double neto, double impuesto) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("casilla", casilla);
        item.put("concepto", concepto);
        item.put("bruto", redondear(bruto));
        item.put("neto", redondear(neto));
        item.put("impuesto", redondear(impuesto));
        casillas.add(item);
    }

    private void agregarCasillaSimple(List<Map<String, Object>> casillas, String casilla, String concepto,
            double valor) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("casilla", casilla);
        item.put("concepto", concepto);
        item.put("valor", redondear(valor));
        casillas.add(item);
    }

    private void noDisponible(List<Map<String, Object>> noDisponibles, String casilla, String motivo) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("casilla", casilla);
        item.put("motivo", motivo);
        noDisponibles.add(item);
    }

    private double nvl(Double valor) {
        return valor != null ? valor.doubleValue() : 0.0;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
