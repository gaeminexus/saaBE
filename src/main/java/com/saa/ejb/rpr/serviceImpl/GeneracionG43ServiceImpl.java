package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.saa.ejb.rpr.dao.HistoricoG42DaoService;
import com.saa.ejb.rpr.dao.ParticipeCesanteG43DaoService;
import com.saa.ejb.rpr.dao.SujetoReportadoG41DaoService;
import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.EntidadService;
import com.saa.ejb.crd.service.ParticipeService;
import com.saa.model.crd.Participe;
import com.saa.rubros.EstadoParticipeEntidad;
import com.saa.ejb.rpr.service.DetalleEjecucionReporteService;
import com.saa.ejb.rpr.service.EjecucionReporteService;
import com.saa.ejb.rpr.service.GeneracionG43Service;
import com.saa.ejb.rpr.service.SaldoCuentaG42Service;
import com.saa.model.crd.Entidad;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.EjecucionReporte;
import com.saa.model.rpr.HistoricoG42;
import com.saa.model.rpr.ParticipeCesanteG43;
import com.saa.model.rpr.SaldoCuentaG42;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG43ServiceImpl implements GeneracionG43Service {

    @EJB private EjecucionReporteService        ejrcService;
    @EJB private DetalleEjecucionReporteService ejrdService;
    @EJB private SaldoCuentaG42Service          saldoG42Service;
    @EJB private HistoricoG42DaoService         historicoG42DaoService;
    @EJB private ParticipeCesanteG43DaoService  cg43DaoService;
    @EJB private AporteService                  aporteService;
    @EJB private EntidadService                 entidadService;
    @EJB private ParticipeService               participeService;
    @EJB private SujetoReportadoG41DaoService   sujetoReportadoG41Dao;

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("Ingresa al metodo generar G43");

        // -------------------------------------------------------
        // 1. Obtener la cabecera EJRC actual y calcular mes/anio anterior
        // -------------------------------------------------------
        EjecucionReporte ejrcActual = detalle.getEjecucionReporte();
        long mesActual  = ejrcActual.getMes();
        long anioActual = ejrcActual.getAnio();

        long mesPrevio  = (mesActual == 1) ? 12 : mesActual - 1;
        long anioPrevio = (mesActual == 1) ? anioActual - 1 : anioActual;

        System.out.println("G43 - Mes actual: " + mesActual + "/" + anioActual
                + " | Mes anterior buscado: " + mesPrevio + "/" + anioPrevio);

        // -------------------------------------------------------
        // 2. Obtener el EJRD del G42 actual
        // -------------------------------------------------------
        DetalleEjecucionReporte ejrdG42Actual =
                ejrdService.selectByEjecucionYTipo(ejrcActual.getCodigo(), "G42");

        if (ejrdG42Actual == null) {
            throw new Exception("G43: No se encontro el EJRD del G42 del mes actual. "
                    + "El G42 debe haberse generado antes que el G43.");
        }
        Long codigoDetalleActual = ejrdG42Actual.getCodigo();

        // -------------------------------------------------------
        // 3. Obtener los cesantes directamente en BD con NOT EXISTS
        // -------------------------------------------------------
        List<EjecucionReporte> ejecucionesPrevias = null;
        try {
            ejecucionesPrevias = ejrcService.selectByMesAnio(mesPrevio, anioPrevio);
        } catch (Throwable e) {
            ejecucionesPrevias = null;
        }

        List<SaldoCuentaG42> cesantesG42  = null;
        List<HistoricoG42>   cesantesHist = null;
        boolean usandoHistorico = false;

        if (ejecucionesPrevias != null && !ejecucionesPrevias.isEmpty()) {
            EjecucionReporte ejrcPrevio = ejecucionesPrevias.get(0);
            DetalleEjecucionReporte ejrdG42Previo =
                    ejrdService.selectByEjecucionYTipo(ejrcPrevio.getCodigo(), "G42");

            if (ejrdG42Previo != null) {
                cesantesG42 = saldoG42Service.selectCesantesDesdeG42Previo(
                        ejrdG42Previo.getCodigo(), codigoDetalleActual);
                System.out.println("G43 - Camino A (CG42 mes previo): "
                        + cesantesG42.size() + " cesantes encontrados en BD");
            } else {
                usandoHistorico = true;
            }
        } else {
            usandoHistorico = true;
        }

        if (usandoHistorico) {
            cesantesHist = historicoG42DaoService.selectCesantesDesdeHistorico(codigoDetalleActual);
            System.out.println("G43 - Camino B (HistoricoG42): "
                    + cesantesHist.size() + " cesantes encontrados en BD");
        }

        // -------------------------------------------------------
        // 4. Mes de ejecución
        // -------------------------------------------------------
        LocalDate inicioMes = LocalDate.of((int) anioActual, (int) mesActual, 1);
        LocalDate corte     = inicioMes.plusMonths(1).minusDays(1);

        // D-038: los jubilados no son cesantes. Un jubilado cuya cuenta se agota también sale de la G42
        // (junio 2025: los 2 "cesantes" eran jubilados pasivos de la G44 con su último pago de pensión).
        Set<String> jubiladosDelMes = new HashSet<>();
        DetalleEjecucionReporte ejrdG44 = ejrdService.selectByEjecucionYTipo(ejrcActual.getCodigo(), "G44");
        if (ejrdG44 != null) {
            for (String id : sujetoReportadoG41Dao.selectIdentificacionesG44(ejrdG44.getCodigo())) jubiladosDelMes.add(id.trim());
        }

        // -------------------------------------------------------
        // 5. Insertar los cesantes en CG43
        long contador = 0L;

        if (cesantesG42 != null) {
            for (SaldoCuentaG42 g42 : cesantesG42) {
                double saldoPrevio = nz(g42.getSaldoAportePatronal()) + nz(g42.getSaldoAportePersonal())
                        + nz(g42.getSaldoAporteVoluntario()) + nz(g42.getRendimiento());
                if (guardarCesante(g42.getIdentificacion(), g42.getTipoIdentificacion(), saldoPrevio,
                        detalle, inicioMes, corte, jubiladosDelMes)) contador++;
            }
        }

        if (cesantesHist != null) {
            for (HistoricoG42 h : cesantesHist) {
                double saldoPrevio = nz(h.getSaldoAportePatronal()) + nz(h.getSaldoAportePersonal())
                        + nz(h.getSaldoAporteVoluntario()) + nz(h.getRendimiento());
                if (guardarCesante(h.getIdentificacion(), h.getTipoIdentificacion(), saldoPrevio,
                        detalle, inicioMes, corte, jubiladosDelMes)) contador++;
            }
        }

        if (contador == 0) {
            System.out.println("G43 - Sin cesantes este mes. G43 vacio, OK.");
        } else {
            System.out.println("G43 generado con " + contador + " registros");
        }
        return contador;
    }

    /** Estados de entidad que son jubilados (EstadoParticipeEntidad): no van en la G43 aunque salgan de la G42. */
    static final Set<Long> ESTADOS_JUBILADO = Set.of(
            (long) EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO,
            (long) EstadoParticipeEntidad.JUBILADO_APORTANTE,
            (long) EstadoParticipeEntidad.JUBILADO_PASIVO);

    /**
     * Registra un cesante en CG43 (manual de catastros §2.4 y §4.4), D-038:
     * <ul>
     *   <li>fecha de liquidación: la del último movimiento negativo del mes (la devolución real); si no hay, el corte;</li>
     *   <li>fecha de término de la relación laboral: {@code PRTCFCSL} si existe y es anterior a la liquidación;
     *       si no, el día anterior a la liquidación (mismo criterio que la G43 de mayo 2025: 8 de 11);</li>
     *   <li>imposiciones: meses de devengo con aporte hasta el corte (personales 9/11, patronales 13/14);</li>
     *   <li>saldo de la cuenta individual "al momento de la liquidación": el saldo de la G42 anterior (patronal +
     *       personal + voluntario + rendimiento); si no lo hay, lo liquidado en el mes;</li>
     *   <li>valores compensados: cruce con préstamos del mes (APRTTPMV 4); pagados: lo liquidado menos lo compensado.
     *       Control §4.4: los dos ≥ 0 y ≤ saldo (si lo liquidado supera el saldo previo, el saldo es lo liquidado).</li>
     * </ul>
     * No registra a los jubilados (G44 del mes o estado de jubilado).
     */
    private boolean guardarCesante(String identificacion, String tipoIdentificacion, double saldoPrevio,
            DetalleEjecucionReporte detalle, LocalDate inicioMes, LocalDate corte, Set<String> jubiladosDelMes)
            throws Throwable {
        String id = identificacion != null ? identificacion.trim() : null;
        if (id == null) return false;
        if (jubiladosDelMes.contains(id)) {
            System.out.println("G43 - se omite un jubilado de la G44 del mes (no es cesante)");
            return false;
        }
        Entidad entidad = null;
        try {
            entidad = entidadService.selectByNumeroIdentificacion(id);
        } catch (Throwable e) {
            System.out.println("G43 - No se encontro entidad para el sujeto");
        }
        if (entidad != null && entidad.getIdEstado() != null && ESTADOS_JUBILADO.contains(entidad.getIdEstado())) {
            System.out.println("G43 - se omite la entidad " + entidad.getCodigo() + " (estado de jubilado " + entidad.getIdEstado() + ")");
            return false;
        }

        ParticipeCesanteG43 cesante = new ParticipeCesanteG43();
        cesante.setIdentificacion(id);
        cesante.setTipoIdentificacion(tipoIdentificacion != null ? tipoIdentificacion : "C");
        cesante.setDetalleEjecucion(detalle);

        LocalDate fechaLiquidacion = corte;
        double liquidado = 0.0, compensado = 0.0;
        long impPersonales = 0L, impPatronales = 0L;
        LocalDate fechaSalida = null;
        if (entidad != null) {
            Long codigoEntidad = entidad.getCodigo();
            impPersonales = nzl(aporteService.selectCountImposicionesPersonalesPorEntidad(codigoEntidad, corte));
            impPatronales = nzl(aporteService.selectCountImposicionesPatronalesPorEntidad(codigoEntidad, corte));
            Object[] liq = aporteService.selectLiquidacionMesPorEntidad(codigoEntidad, inicioMes, corte);
            if (liq != null) {
                if (liq[0] != null) fechaLiquidacion = aFecha(liq[0]);
                liquidado  = liq[1] != null ? Math.abs(((Number) liq[1]).doubleValue()) : 0.0;
                compensado = liq[2] != null ? Math.abs(((Number) liq[2]).doubleValue()) : 0.0;
            }
            List<Participe> participes = participeService.selectByEntidad(codigoEntidad);
            if (participes != null && !participes.isEmpty() && participes.get(0).getFechaSalida() != null) {
                fechaSalida = participes.get(0).getFechaSalida().toLocalDate();
            }
        }
        LocalDate fechaTermino = (fechaSalida != null && fechaSalida.isBefore(fechaLiquidacion))
                ? fechaSalida : fechaLiquidacion.minusDays(1);

        double saldo = saldoPrevio > 0 ? saldoPrevio : liquidado;
        if (liquidado > saldo) saldo = liquidado;
        cesante.setFechaLiquidacion(fechaLiquidacion);
        cesante.setFechaTerminoRelacionLaboral(fechaTermino);
        cesante.setNumeroImposicionesPersonales(impPersonales);
        cesante.setNumeroImposicionesPatronales(impPatronales);
        cesante.setSaldoCuentaIndividual(redondear(saldo));
        cesante.setValoresCompensados(redondear(compensado));
        cesante.setValoresPagados(redondear(Math.max(0.0, liquidado - compensado)));
        cg43DaoService.save(cesante, null);
        return true;
    }

    private static double nz(Double d) { return d != null ? d : 0.0; }
    private static long nzl(Long l) { return l != null ? l : 0L; }
    private static double redondear(double v) { return Math.round(v * 100.0) / 100.0; }

    private static LocalDate aFecha(Object o) {
        if (o instanceof java.sql.Timestamp) return ((java.sql.Timestamp) o).toLocalDateTime().toLocalDate();
        if (o instanceof java.sql.Date) return ((java.sql.Date) o).toLocalDate();
        if (o instanceof LocalDateTime) return ((LocalDateTime) o).toLocalDate();
        if (o instanceof LocalDate) return (LocalDate) o;
        if (o instanceof java.util.Date) return new java.sql.Date(((java.util.Date) o).getTime()).toLocalDate();
        return null;
    }
}
