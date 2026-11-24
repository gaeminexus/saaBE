package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.PrestamoCanceladoDaoService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.ReportePrestamoCanceladoService;
import com.saa.ejb.crd.service.SustitucionPrestamoService;
import com.saa.ejb.crd.service.dto.FiltroPrestamosCancelados;
import com.saa.ejb.crd.service.dto.PrestamoCanceladoFila;
import com.saa.ejb.crd.service.dto.ReportePrestamosCancelados;
import com.saa.ejb.crd.service.dto.ResumenFormaCancelacion;
import com.saa.ejb.reporte.service.ReporteService;
import com.saa.rubros.CrdTipoOperacionCobro;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.Filiales;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Reporte de préstamos cancelados. Ver {@link ReportePrestamoCanceladoService} y el contrato
 * docs/logica-negocio/crd/API-REPORTE-PRESTAMOS-CANCELADOS.md.
 *
 * <p>Todo el acceso a datos es por lotes (una consulta por tipo de dato para TODOS los préstamos del rango,
 * nunca una por préstamo). Aquí solo se clasifica y se arman las filas.</p>
 */
@Stateless
public class ReportePrestamoCanceladoServiceImpl implements ReportePrestamoCanceladoService {

    private static final String MODULO_REPORTE = "crd";
    private static final String NOMBRE_REPORTE = "RPRT_PRCN_LSTD";

    private static final String TIPO_MIGRACION = "MIGRACION";
    private static final String TIPO_DEP = "DEP";
    private static final String TIPO_CARGA = "CARGA";
    private static final double TOLERANCIA = 0.005;
    private static final int MAX_OBSERVACION = 500;

    /** Marca de la precancelación pagada con un préstamo nuevo (ProcesoPagoPrestamoServiceImpl.marcaPrecancelacionConPrestamo). */
    private static final Pattern MARCA_D021 = Pattern.compile("\\[D-021 pr\\u00e9stamo nuevo (\\d+)\\]");

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> DESCRIPCION_FORMA = new LinkedHashMap<>();
    static {
        DESCRIPCION_FORMA.put(FORMA_PAGO_ULTIMA_CUOTA, "Pago de la última cuota");
        DESCRIPCION_FORMA.put(FORMA_ABONO_CAPITAL, "Abono a capital que terminó la deuda");
        DESCRIPCION_FORMA.put(FORMA_PRECANCELACION, "Precancelación");
        DESCRIPCION_FORMA.put(FORMA_PRECANCELACION_CON_PRESTAMO, "Precancelado con un préstamo nuevo");
        DESCRIPCION_FORMA.put(FORMA_ACUERDO_CONDONACION, "Acuerdo con condonación");
        DESCRIPCION_FORMA.put(FORMA_NOVACION, "Novación");
        DESCRIPCION_FORMA.put(FORMA_REESTRUCTURACION, "Reestructuración");
        DESCRIPCION_FORMA.put(FORMA_SIN_DETERMINAR, "Sin operación registrada");
    }

    private static final List<Long> ESTADOS_VALIDOS = List.of(
        (long) EstadoPrestamo.CANCELADO, (long) EstadoPrestamo.CANCELADO_ANTICIPADO,
        (long) EstadoPrestamo.CANCELADO_POR_NOVACION, (long) EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION);

    @EJB
    private PrestamoCanceladoDaoService dao;

    @EJB
    private ReporteService reporteService;

    /** El filtro ya validado y normalizado. */
    private static final class FiltroValido {
        LocalDate desde;
        LocalDate hasta;
        Long idProducto;
        Long idFilial;
        List<Long> estados;
        Set<String> formas;
        String usuario;
    }

    // =====================================================================
    // API
    // =====================================================================

    @Override
    public ReportePrestamosCancelados consultar(FiltroPrestamosCancelados filtro) throws Throwable {
        System.out.println("ReportePrestamoCanceladoService.consultar - desde: "
            + (filtro != null ? filtro.getFechaDesde() : null) + " - hasta: "
            + (filtro != null ? filtro.getFechaHasta() : null));
        FiltroValido f = validar(filtro);

        List<Object[]> candidatos = dao.candidatos(java.sql.Date.valueOf(f.desde), java.sql.Date.valueOf(f.hasta),
            f.idProducto, f.idFilial, f.estados);
        long sinFecha = dao.contarSinFecha(f.idProducto, f.idFilial, f.estados);

        List<PrestamoCanceladoFila> filas = armarFilas(candidatos);
        if (!f.formas.isEmpty()) {
            List<PrestamoCanceladoFila> filtradas = new ArrayList<>();
            for (PrestamoCanceladoFila fila : filas) {
                if (f.formas.contains(fila.getForma())) {
                    filtradas.add(fila);
                }
            }
            filas = filtradas;
        }
        filas.sort(Comparator
            .comparing(PrestamoCanceladoFila::getFechaCancelacion, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PrestamoCanceladoFila::getNumeroOperacion, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PrestamoCanceladoFila::getIdPrestamo, Comparator.nullsLast(Comparator.naturalOrder())));

        ReportePrestamosCancelados r = new ReportePrestamosCancelados();
        r.setFechaDesde(f.desde.format(FMT_FECHA));
        r.setFechaHasta(f.hasta.format(FMT_FECHA));
        r.setGenerado(LocalDateTime.now().withNano(0).toString());
        r.setCantidad((long) filas.size());
        r.setCanceladosSinFecha(sinFecha);
        r.setFilas(filas);
        resumir(r, filas);
        return r;
    }

    @Override
    public byte[] pdf(FiltroPrestamosCancelados filtro) throws Throwable {
        System.out.println("ReportePrestamoCanceladoService.pdf - desde: "
            + (filtro != null ? filtro.getFechaDesde() : null) + " - hasta: "
            + (filtro != null ? filtro.getFechaHasta() : null));
        FiltroValido f = validar(filtro);
        ReportePrestamosCancelados r = consultar(filtro);

        // Una COPIA ordenada por forma (orden de §4) y fecha: la respuesta de pantalla no se altera.
        List<PrestamoCanceladoFila> filas = new ArrayList<>(r.getFilas());
        filas.sort(Comparator
            .comparingInt((PrestamoCanceladoFila x) -> FORMAS_EN_ORDEN.indexOf(x.getForma()))
            .thenComparing(PrestamoCanceladoFila::getFechaCancelacion, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(PrestamoCanceladoFila::getNumeroOperacion, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("FECHA_DESDE", r.getFechaDesde());
        parametros.put("FECHA_HASTA", r.getFechaHasta());
        parametros.put("GENERADO", r.getGenerado().replace('T', ' '));
        parametros.put("USUARIO", f.usuario != null ? f.usuario : "");
        parametros.put("FILTROS", textoFiltros(f));
        parametros.put("CANTIDAD", r.getCantidad());
        parametros.put("CANCELADOS_SIN_FECHA", r.getCanceladosSinFecha());
        return reporteService.generarReporteDesdeColeccion(MODULO_REPORTE, NOMBRE_REPORTE, parametros, filas, "PDF");
    }

    // =====================================================================
    // Validación (§2.2)
    // =====================================================================

    private FiltroValido validar(FiltroPrestamosCancelados filtro) {
        if (filtro == null) {
            throw new IncomeException("El filtro es obligatorio: falta fechaDesde y fechaHasta");
        }
        FiltroValido f = new FiltroValido();
        f.desde = parseFecha("fechaDesde", filtro.getFechaDesde());
        f.hasta = parseFecha("fechaHasta", filtro.getFechaHasta());
        if (f.desde.isAfter(f.hasta)) {
            throw new IncomeException("fechaDesde (" + f.desde + ") no puede ser posterior a fechaHasta (" + f.hasta + ")");
        }
        f.idProducto = filtro.getIdProducto();
        f.idFilial = filtro.getIdFilial();
        f.usuario = filtro.getUsuario();

        f.estados = new ArrayList<>();
        if (filtro.getEstados() != null) {
            for (Long e : filtro.getEstados()) {
                if (e == null || !ESTADOS_VALIDOS.contains(e)) {
                    throw new IncomeException("estados contiene un valor no válido: " + e + " (válidos: 3, 4, 5, 13)");
                }
                if (!f.estados.contains(e)) {
                    f.estados.add(e);
                }
            }
        }
        if (f.estados.isEmpty()) {
            f.estados.addAll(ESTADOS_VALIDOS);
        }

        f.formas = new LinkedHashSet<>();
        if (filtro.getFormas() != null) {
            for (String forma : filtro.getFormas()) {
                if (forma == null || !FORMAS_EN_ORDEN.contains(forma)) {
                    throw new IncomeException("formas contiene un código desconocido: " + forma);
                }
                f.formas.add(forma);
            }
        }
        return f;
    }

    private static LocalDate parseFecha(String campo, String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IncomeException(campo + " es obligatoria (formato yyyy-MM-dd)");
        }
        try {
            return LocalDate.parse(valor.trim(), FMT_FECHA);
        } catch (DateTimeParseException e) {
            throw new IncomeException(campo + " no tiene el formato yyyy-MM-dd: " + valor);
        }
    }

    /** Texto de filtros del encabezado del PDF: con los NOMBRES de producto y filial; «Producto #id» si no existe. */
    private String textoFiltros(FiltroValido f) {
        String producto = "todos";
        if (f.idProducto != null) {
            String nombre = dao.nombreProducto(f.idProducto);
            producto = nombre != null ? nombre : "Producto #" + f.idProducto;
        }
        String filial = "todas";
        if (f.idFilial != null) {
            String nombre = dao.nombreFilial(f.idFilial);
            filial = nombre != null ? nombre : "Filial #" + f.idFilial;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Producto: ").append(producto);
        sb.append("  |  Filial: ").append(filial);
        sb.append("  |  Estados: ");
        for (int i = 0; i < f.estados.size(); i++) {
            sb.append(i > 0 ? ", " : "").append(textoEstado(f.estados.get(i)));
        }
        sb.append("  |  Formas: ");
        if (f.formas.isEmpty()) {
            sb.append("todas");
        } else {
            int i = 0;
            for (String forma : f.formas) {
                sb.append(i++ > 0 ? ", " : "").append(DESCRIPCION_FORMA.get(forma));
            }
        }
        return sb.toString();
    }

    // =====================================================================
    // Armado de filas
    // =====================================================================

    private List<PrestamoCanceladoFila> armarFilas(List<Object[]> candidatos) {
        List<PrestamoCanceladoFila> filas = new ArrayList<>();
        if (candidatos.isEmpty()) {
            return filas;
        }
        List<Long> ids = new ArrayList<>();
        for (Object[] c : candidatos) {
            ids.add(lng(c[0]));
        }

        // ---- Una consulta por tipo de dato, para todos los préstamos ----
        Map<Long, Object[]> datos = porPrimeraColumna(dao.datosPrestamos(ids));
        Map<Long, Object[]> pagos = porPrimeraColumna(dao.agregadosPagos(ids));
        Map<Long, Object[]> cuotas = porPrimeraColumna(dao.agregadosCuotas(ids));
        Map<Long, Object[]> opFinal = porPrimeraColumna(dao.operacionFinal(ids));
        Map<Long, Object[]> sust = porPrimeraColumna(dao.eventosSustitucion(ids));

        Map<Long, List<Object[]>> aportes = new HashMap<>();
        for (Object[] a : dao.aportesPorPago(ids)) {
            aportes.computeIfAbsent(lng(a[0]), k -> new ArrayList<>()).add(a);
        }

        List<Long> idsEvento = new ArrayList<>();
        List<Long> idsPago = new ArrayList<>();
        for (Object[] o : opFinal.values()) {
            ("E".equals(o[1]) ? idsEvento : idsPago).add(lng(o[2]));
        }
        List<Long> idsEventoTodos = new ArrayList<>(idsEvento);
        for (Object[] s : sust.values()) {
            idsEventoTodos.add(lng(s[1]));
        }
        Map<Long, Object[]> eventos = porPrimeraColumna(dao.detalleEventos(idsEventoTodos));
        Map<Long, Object[]> pagosSinEvento = porPrimeraColumna(dao.detallePagosSinEvento(idsPago));
        Map<Long, Object[]> cobros = porPrimeraColumna(dao.cobrosPorEvento(idsEvento));
        Map<Long, Object[]> acuerdos = porPrimeraColumna(dao.acuerdosPorEvento(idsEvento));

        // Préstamo nuevo: por PRSTPROR (novación/reestructuración) o por la marca D-021 de la precancelación.
        List<Long> idsSustituidos = new ArrayList<>();
        List<Long> idsNuevosD = new ArrayList<>();
        for (Object[] c : candidatos) {
            long est = lng(c[1]);
            if (est == EstadoPrestamo.CANCELADO_POR_NOVACION || est == EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION) {
                idsSustituidos.add(lng(c[0]));
            }
        }
        for (Object[] o : opFinal.values()) {
            if ("E".equals(o[1])) {
                Object[] ev = eventos.get(lng(o[2]));
                Long n = ev != null ? numeroMarcaD021(str(ev[1]), str(ev[5])) : null;
                if (n != null) {
                    idsNuevosD.add(n);
                }
            }
        }
        Map<Long, Object[]> nuevosPorOriginal = new HashMap<>();
        for (Object[] n : dao.nuevosPorOriginal(idsSustituidos)) {
            Long original = lng(n[0]);
            Object[] previo = nuevosPorOriginal.get(original);
            if (previo == null || lng(n[1]) > lng(previo[1])) {
                nuevosPorOriginal.put(original, n);
            }
        }
        Map<Long, Object[]> nuevosD = new HashMap<>();
        for (Object[] n : dao.prestamosPorCodigo(idsNuevosD)) {
            nuevosD.put(lng(n[1]), n);
        }

        // ---- Una fila por préstamo ----
        for (Object[] c : candidatos) {
            Long id = lng(c[0]);
            Object[] dp = datos.get(id);
            if (dp == null) {
                continue;
            }
            PrestamoCanceladoFila f = new PrestamoCanceladoFila();
            f.setIdPrestamo(id);
            f.setNumeroOperacion(lng(dp[1]));
            f.setCedula(str(dp[2]));
            f.setNombreSocio(str(dp[3]));
            f.setFilial(str(dp[4]));
            f.setProducto(str(dp[5]));
            long estado = lng(dp[6]);
            f.setEstadoCodigo(estado);
            f.setEstado(textoEstado(estado));
            f.setFechaCancelacion(fecha(c[2]));
            f.setFechaOtorgamiento(fecha(dp[7]));
            f.setFechaInicio(fecha(dp[8]));
            f.setFechaFinPlazo(fecha(dp[9]));
            f.setPlazo(lng(dp[10]));
            f.setTasa(num(dp[11]));
            f.setValorCuota(r2(num(dp[12])));
            f.setMontoSolicitado(r2(num(dp[13])));
            f.setMontoLiquido(r2(num(dp[14])));
            Long idDeudor = lng(dp[15]);

            Object[] cu = cuotas.get(id);
            f.setCuotasPagadas(cu != null ? lng(cu[1]) : 0L);
            f.setCuotasCanceladasAnticipadas(cu != null ? lng(cu[2]) : 0L);
            f.setUltimaCuotaPagada(cu != null ? lng(cu[3]) : null);

            Object[] pg = pagos.get(id);
            f.setNumeroPagos(pg != null ? lng(pg[1]) : 0L);
            f.setFechaPrimerPago(pg != null ? fecha(pg[2]) : null);
            f.setFechaUltimoPago(pg != null ? fecha(pg[3]) : null);
            f.setTotalCapitalPagado(pg != null ? r2(num(pg[4])) : 0.0);
            f.setTotalInteresPagado(pg != null ? r2(num(pg[5])) : 0.0);
            f.setTotalMoraPagada(pg != null ? r2(num(pg[6])) : 0.0);
            f.setTotalInteresVencidoPagado(pg != null ? r2(num(pg[7])) : 0.0);
            f.setTotalDesgravamenPagado(pg != null ? r2(num(pg[8])) : 0.0);
            f.setTotalSeguroIncendioPagado(pg != null ? r2(num(pg[9])) : 0.0);
            f.setTotalSaldoOtrosPagado(pg != null ? r2(num(pg[10])) : 0.0);
            f.setTotalPagado(pg != null ? r2(num(pg[11])) : 0.0);
            f.setTieneMigracion(pg != null && lng(pg[12]) != null && lng(pg[12]) > 0 ? "SI" : "NO");

            double aportesTotal = 0.0;
            double aportesGarante = 0.0;
            List<Object[]> lista = aportes.get(id);
            if (lista != null) {
                for (Object[] a : lista) {
                    aportesTotal += num(a[4]);
                    aportesGarante += num(a[5]);
                }
            }
            f.setTotalAportesUsados(r2(aportesTotal));
            f.setTotalAportesGarante(r2(aportesGarante));

            // Valores por omisión de la operación final (se pisan abajo).
            f.setValorOperacionFinal(0.0);
            f.setEfectivoOperacionFinal(0.0);
            f.setAportesOperacionFinal(0.0);
            f.setAportesGaranteOperacionFinal(0.0);
            f.setValorCondonado(0.0);

            List<String> advertencias = new ArrayList<>();
            String origenDao = str(c[3]);
            boolean sustitucion = estado == EstadoPrestamo.CANCELADO_POR_NOVACION
                || estado == EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION;

            if (sustitucion) {
                completarSustitucion(f, estado, origenDao, sust.get(id), eventos, nuevosPorOriginal.get(id), advertencias);
            } else {
                Object[] sustEnTerminal = sust.get(id);
                if (sustEnTerminal != null) {
                    advertencias.add("evento de sustitución en un préstamo 3/4");
                }
                completarPago(f, estado, id, idDeudor, origenDao, opFinal.get(id), eventos, pagosSinEvento,
                    cobros, acuerdos, aportes.get(id), nuevosD, advertencias);
            }
            f.setObservacionReporte(advertencias.isEmpty() ? null : String.join("; ", advertencias));
            filas.add(f);
        }
        return filas;
    }

    /** Estados 5/13: la cancelación es el evento de sustitución; no entra dinero. */
    private void completarSustitucion(PrestamoCanceladoFila f, long estado, String origenDao, Object[] sustEvento,
            Map<Long, Object[]> eventos, Object[] nuevo, List<String> advertencias) {
        String forma = estado == EstadoPrestamo.CANCELADO_POR_NOVACION ? FORMA_NOVACION : FORMA_REESTRUCTURACION;
        f.setForma(forma);
        f.setFormaDescripcion(DESCRIPCION_FORMA.get(forma));
        f.setCanal(CANAL_PRESTAMO_NUEVO);
        boolean conEvento = "SUSTITUCION".equals(origenDao);
        f.setOrigenFecha(conEvento ? ORIGEN_SUSTITUCION : ORIGEN_CUOTA);
        if (!conEvento) {
            advertencias.add("la fecha sale de las cuotas: sin operación registrada");
        }
        Object[] ev = sustEvento != null ? eventos.get(lng(sustEvento[1])) : null;
        if (ev != null) {
            f.setTipoOperacionFinal(str(ev[1]));
            f.setFechaOperacionFinal(fechaHora(ev[2]));
            f.setUsuarioOperacionFinal(str(ev[6]));
            f.setIdEvento(lng(ev[0]));
            f.setIdAsiento(lng(ev[4]));
            f.setObservacionOperacionFinal(observacion(str(ev[5])));
        }
        if (nuevo != null) {
            f.setIdPrestamoNuevo(lng(nuevo[1]));
            f.setNumeroOperacionNuevo(lng(nuevo[2]));
            f.setMontoPrestamoNuevo(r2(num(nuevo[3])));
            f.setValorSustitucion(r2(num(nuevo[4])));
        }
    }

    /** Estados 3/4: la operación final (§4.1) decide la forma, el canal y el origen de la fecha. */
    private void completarPago(PrestamoCanceladoFila f, long estado, Long id, Long idDeudor, String origenDao,
            Object[] op, Map<Long, Object[]> eventos, Map<Long, Object[]> pagosSinEvento,
            Map<Long, Object[]> cobros, Map<Long, Object[]> acuerdos, List<Object[]> aportesPrestamo,
            Map<Long, Object[]> nuevosD, List<String> advertencias) {

        if (op == null) {
            f.setForma(FORMA_SIN_DETERMINAR);
            f.setFormaDescripcion(DESCRIPCION_FORMA.get(FORMA_SIN_DETERMINAR));
            f.setCanal(CANAL_SIN_DATO);
            f.setOrigenFecha(ORIGEN_CUOTA);
            advertencias.add("la fecha sale de las cuotas: sin operación registrada");
            return;
        }

        Long codigo = lng(op[2]);
        boolean esEvento = "E".equals(op[1]);
        List<Object[]> aportesOp = new ArrayList<>();
        double aportes = 0.0;
        double aportesGarante = 0.0;

        if (esEvento) {
            Object[] ev = eventos.get(codigo);
            if (ev == null) {
                f.setForma(FORMA_SIN_DETERMINAR);
                f.setFormaDescripcion(DESCRIPCION_FORMA.get(FORMA_SIN_DETERMINAR));
                f.setCanal(CANAL_SIN_DATO);
                f.setOrigenFecha(ORIGEN_EVENTO);
                return;
            }
            String tipo = str(ev[1]);
            String observacion = str(ev[5]);
            double valor = r2(num(ev[3]));
            if (aportesPrestamo != null) {
                for (Object[] a : aportesPrestamo) {
                    if (a[2] != null && codigo.equals(lng(a[2]))) {
                        aportes += num(a[4]);
                        aportesGarante += num(a[5]);
                    }
                }
            }
            aportes = r2(aportes);
            aportesGarante = r2(aportesGarante);

            Object[] cobro = cobros.get(codigo);
            Object[] acuerdo = acuerdos.get(codigo);
            Long nuevoD = numeroMarcaD021(tipo, observacion);

            String forma;
            if (ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL.equals(tipo)) {
                forma = FORMA_ABONO_CAPITAL;
            } else if (ProcesoPagoPrestamoService.TIPO_PRECANCELACION.equals(tipo)) {
                forma = nuevoD != null ? FORMA_PRECANCELACION_CON_PRESTAMO : FORMA_PRECANCELACION;
            } else if (CrdTipoOperacionCobro.ACUERDO_CONDONACION.equals(tipo)) {
                forma = FORMA_ACUERDO_CONDONACION;
            } else {
                forma = FORMA_PAGO_ULTIMA_CUOTA;
            }
            if (estado == EstadoPrestamo.CANCELADO_ANTICIPADO && !ProcesoPagoPrestamoService.TIPO_PRECANCELACION.equals(tipo)) {
                advertencias.add("estado 4 sin evento de precancelación");
            }
            f.setForma(forma);
            f.setFormaDescripcion(DESCRIPCION_FORMA.get(forma));
            f.setOrigenFecha(ORIGEN_EVENTO);
            f.setTipoOperacionFinal(tipo);
            f.setFechaOperacionFinal(fechaHora(ev[2]));
            f.setValorOperacionFinal(valor);
            f.setUsuarioOperacionFinal(str(ev[6]));
            f.setIdEvento(codigo);
            f.setObservacionOperacionFinal(observacion(observacion));
            f.setAportesOperacionFinal(aportes);
            f.setAportesGaranteOperacionFinal(aportesGarante);

            double efectivo = Math.max(valor - aportes, 0.0);
            if (acuerdo != null) {
                efectivo = r2(num(acuerdo[5]));
                f.setValorCondonado(r2(num(acuerdo[3])));
            }
            f.setEfectivoOperacionFinal(r2(efectivo));

            f.setIdCobro(cobro != null ? lng(cobro[1]) : (acuerdo != null ? lng(acuerdo[6]) : null));
            Long asiento = lng(ev[4]);
            if (asiento == null && cobro != null) {
                asiento = lng(cobro[3]);
            }
            f.setIdAsiento(asiento);

            // Canal (§4.2)
            String canal;
            boolean mixto = cobro != null && CrdTipoOperacionCobro.COBRO_MIXTO.equals(str(cobro[2]));
            if (FORMA_PRECANCELACION_CON_PRESTAMO.equals(forma)) {
                canal = CANAL_PRESTAMO_NUEVO;
            } else if (FORMA_ACUERDO_CONDONACION.equals(forma)) {
                canal = CANAL_CONDONACION;
            } else if (mixto || (efectivo > TOLERANCIA && aportes > TOLERANCIA)) {
                canal = CANAL_MIXTO;
            } else if (aportes > TOLERANCIA && aportesGarante > TOLERANCIA) {
                canal = CANAL_APORTES_GARANTE;
            } else if (aportes > TOLERANCIA) {
                canal = CANAL_APORTES;
            } else {
                canal = CANAL_CAJA;
            }
            f.setCanal(canal);

            if (nuevoD != null) {
                Object[] nuevo = nuevosD.get(nuevoD);
                f.setIdPrestamoNuevo(nuevoD);
                f.setValorSustitucion(valor);
                if (nuevo != null) {
                    f.setNumeroOperacionNuevo(lng(nuevo[2]));
                    f.setMontoPrestamoNuevo(r2(num(nuevo[3])));
                }
            }
            return;
        }

        // Operación final = pago sin evento (carga Petro/ARCH o migración)
        Object[] pg = pagosSinEvento.get(codigo);
        if (pg == null) {
            f.setForma(FORMA_SIN_DETERMINAR);
            f.setFormaDescripcion(DESCRIPCION_FORMA.get(FORMA_SIN_DETERMINAR));
            f.setCanal(CANAL_SIN_DATO);
            f.setOrigenFecha(ORIGEN_PAGO_CARGA);
            return;
        }
        String tipoPago = str(pg[1]);
        LocalDateTime fch = aFechaHora(pg[2]);
        if (aportesPrestamo != null) {
            for (Object[] a : aportesPrestamo) {
                if (a[2] == null && fch != null && fch.equals(aFechaHora(a[3]))) {
                    aportes += num(a[4]);
                    aportesGarante += num(a[5]);
                }
            }
        }
        double valor = r2(num(pg[3]));
        boolean migracion = TIPO_MIGRACION.equals(tipoPago) || TIPO_DEP.equals(tipoPago);
        Long filialCarga = lng(pg[7]);

        if (estado == EstadoPrestamo.CANCELADO_ANTICIPADO) {
            advertencias.add("estado 4 sin evento de precancelación");
        }
        f.setForma(FORMA_PAGO_ULTIMA_CUOTA);
        f.setFormaDescripcion(DESCRIPCION_FORMA.get(FORMA_PAGO_ULTIMA_CUOTA));
        f.setOrigenFecha(migracion ? ORIGEN_MIGRACION : ORIGEN_PAGO_CARGA);
        f.setTipoOperacionFinal(tipoPago != null ? tipoPago : TIPO_CARGA);
        f.setFechaOperacionFinal(fechaHora(pg[2]));
        f.setValorOperacionFinal(valor);
        f.setAportesOperacionFinal(r2(aportes));
        f.setAportesGaranteOperacionFinal(r2(aportesGarante));
        f.setEfectivoOperacionFinal(r2(Math.max(valor - aportes, 0.0)));
        f.setUsuarioOperacionFinal(str(pg[4]));
        f.setObservacionOperacionFinal(observacion(str(pg[5])));
        f.setIdCargaArchivo(lng(pg[6]));
        f.setIdAsiento(lng(pg[8]));

        String canal;
        if (migracion) {
            canal = CANAL_MIGRACION;
        } else if ("DESCUENTO_NOMINA".equals(tipoPago)) {
            canal = CANAL_DESCUENTO_ROL_PETRO;
        } else if (filialCarga != null) {
            canal = filialCarga == Filiales.ARCH ? CANAL_ARCH : CANAL_DESCUENTO_ROL_PETRO;
        } else {
            canal = CANAL_SIN_DATO;
        }
        f.setCanal(canal);
    }

    // =====================================================================
    // Resumen y totales
    // =====================================================================

    private void resumir(ReportePrestamosCancelados r, List<PrestamoCanceladoFila> filas) {
        Map<String, ResumenFormaCancelacion> porForma = new LinkedHashMap<>();
        for (String forma : FORMAS_EN_ORDEN) {
            ResumenFormaCancelacion s = vacio();
            s.setForma(forma);
            s.setDescripcion(DESCRIPCION_FORMA.get(forma));
            porForma.put(forma, s);
        }
        ResumenFormaCancelacion totales = vacio();
        for (PrestamoCanceladoFila f : filas) {
            acumular(porForma.get(f.getForma()), f);
            acumular(totales, f);
        }
        List<ResumenFormaCancelacion> resumen = new ArrayList<>();
        for (ResumenFormaCancelacion s : porForma.values()) {
            if (s.getCantidad() > 0) {
                redondear(s);
                resumen.add(s);
            }
        }
        redondear(totales);
        r.setResumenPorForma(resumen);
        r.setTotales(totales);
    }

    private static ResumenFormaCancelacion vacio() {
        ResumenFormaCancelacion s = new ResumenFormaCancelacion();
        s.setCantidad(0L);
        s.setMontoSolicitado(0.0);
        s.setTotalPagado(0.0);
        s.setTotalCapitalPagado(0.0);
        s.setTotalInteresPagado(0.0);
        s.setTotalMoraPagada(0.0);
        s.setTotalCondonado(0.0);
        return s;
    }

    private static void acumular(ResumenFormaCancelacion s, PrestamoCanceladoFila f) {
        s.setCantidad(s.getCantidad() + 1);
        s.setMontoSolicitado(s.getMontoSolicitado() + nvl(f.getMontoSolicitado()));
        s.setTotalPagado(s.getTotalPagado() + nvl(f.getTotalPagado()));
        s.setTotalCapitalPagado(s.getTotalCapitalPagado() + nvl(f.getTotalCapitalPagado()));
        s.setTotalInteresPagado(s.getTotalInteresPagado() + nvl(f.getTotalInteresPagado()));
        s.setTotalMoraPagada(s.getTotalMoraPagada() + nvl(f.getTotalMoraPagada()));
        s.setTotalCondonado(s.getTotalCondonado() + nvl(f.getValorCondonado()));
    }

    private static void redondear(ResumenFormaCancelacion s) {
        s.setMontoSolicitado(r2(s.getMontoSolicitado()));
        s.setTotalPagado(r2(s.getTotalPagado()));
        s.setTotalCapitalPagado(r2(s.getTotalCapitalPagado()));
        s.setTotalInteresPagado(r2(s.getTotalInteresPagado()));
        s.setTotalMoraPagada(r2(s.getTotalMoraPagada()));
        s.setTotalCondonado(r2(s.getTotalCondonado()));
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    /** Indexa las filas de un lote por su primera columna (un id). */
    private static Map<Long, Object[]> porPrimeraColumna(List<Object[]> filas) {
        Map<Long, Object[]> m = new HashMap<>();
        for (Object[] f : filas) {
            m.put(lng(f[0]), f);
        }
        return m;
    }

    private static Long numeroMarcaD021(String tipo, String observacion) {
        if (!ProcesoPagoPrestamoService.TIPO_PRECANCELACION.equals(tipo) || observacion == null) {
            return null;
        }
        Matcher m = MARCA_D021.matcher(observacion);
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }

    private static String textoEstado(long estado) {
        if (estado == EstadoPrestamo.CANCELADO) {
            return "CANCELADO";
        }
        if (estado == EstadoPrestamo.CANCELADO_ANTICIPADO) {
            return "CANCELADO ANTICIPADO";
        }
        if (estado == EstadoPrestamo.CANCELADO_POR_NOVACION) {
            return "CANCELADO POR NOVACION";
        }
        if (estado == EstadoPrestamo.CANCELADO_POR_REESTRUCTURACION) {
            return "CANCELADO POR REESTRUCTURACION";
        }
        return String.valueOf(estado);
    }

    private static String observacion(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.replaceAll("[\\r\\n]+", " ").trim();
        return limpio.length() > MAX_OBSERVACION ? limpio.substring(0, MAX_OBSERVACION) : limpio;
    }

    private static Long lng(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : null;
    }

    private static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
    }

    private static double nvl(Double d) {
        return d != null ? d : 0.0;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private static Double r2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static LocalDateTime aFechaHora(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Timestamp) {
            return ((Timestamp) o).toLocalDateTime();
        }
        if (o instanceof LocalDateTime) {
            return (LocalDateTime) o;
        }
        if (o instanceof LocalDate) {
            return ((LocalDate) o).atStartOfDay();
        }
        if (o instanceof java.sql.Date) {
            return ((java.sql.Date) o).toLocalDate().atStartOfDay();
        }
        if (o instanceof java.util.Date) {
            return LocalDateTime.ofInstant(((java.util.Date) o).toInstant(), ZoneId.systemDefault());
        }
        return null;
    }

    private static String fecha(Object o) {
        LocalDateTime d = aFechaHora(o);
        return d != null ? d.toLocalDate().format(FMT_FECHA) : null;
    }

    private static String fechaHora(Object o) {
        LocalDateTime d = aFechaHora(o);
        return d != null ? d.format(FMT_FECHA_HORA) : null;
    }
}
