package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.ejb.rpr.dao.HistoricoG42DaoService;
import com.saa.ejb.rpr.dao.ParticipeCesanteG43DaoService;
import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.EntidadService;
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
        // 4. Fechas
        // -------------------------------------------------------
        LocalDate fechaLiquidacion = LocalDate.of((int) anioActual, (int) mesActual, 1)
                .plusMonths(1).minusDays(1);  // ultimo dia mes actual
        LocalDate fechaTerminacion = LocalDate.of((int) anioActual, (int) mesActual, 1)
                .minusDays(1);                // ultimo dia mes ANTERIOR

        // Rango del mes de ejecucion para calcular saldo cuenta individual
        LocalDateTime fechaInicioMes = LocalDate.of((int) anioActual, (int) mesActual, 1).atStartOfDay();
        LocalDateTime fechaFinMes    = fechaLiquidacion.atTime(23, 59, 59);

        System.out.println("G43 - Fecha liquidacion: " + fechaLiquidacion
                + " | Fecha terminacion laboral: " + fechaTerminacion);

        // -------------------------------------------------------
        // 5. Insertar los cesantes en CG43
        long contador = 0L;

        if (cesantesG42 != null) {
            for (SaldoCuentaG42 g42 : cesantesG42) {
                ParticipeCesanteG43 cesante = construirCesante(
                        g42.getIdentificacion(), g42.getTipoIdentificacion(),
                        detalle, fechaTerminacion, fechaLiquidacion, fechaInicioMes, fechaFinMes);
                cg43DaoService.save(cesante, null);
                System.out.println("G43 INSERT cesante (desde G42 previo): " + g42.getIdentificacion());
                contador++;
            }
        }

        if (cesantesHist != null) {
            for (HistoricoG42 h : cesantesHist) {
                ParticipeCesanteG43 cesante = construirCesante(
                        h.getIdentificacion(), h.getTipoIdentificacion(),
                        detalle, fechaTerminacion, fechaLiquidacion, fechaInicioMes, fechaFinMes);
                cg43DaoService.save(cesante, null);
                System.out.println("G43 INSERT cesante (desde HistoricoG42): " + h.getIdentificacion());
                contador++;
            }
        }

        if (contador == 0) {
            System.out.println("G43 - Sin cesantes este mes. G43 vacio, OK.");
        } else {
            System.out.println("G43 generado con " + contador + " registros");
        }
        return contador;
    }

    /**
     * Construye un ParticipeCesanteG43 con los campos calculados desde APRT:
     * - numeroImposicionesPersonales : COUNT aportes valor > 0, tipoAporte IN (9, 11)
     * - numeroImposicionesPatronales : COUNT aportes valor > 0, tipoAporte IN (13, 14)
     * - fechaTerminoRelacionLaboral  : ultimo dia del mes anterior
     * - fechaLiquidacion             : ultimo dia del mes actual
     * - saldoCuentaIndividual        : ABS(SUM aportes valor < 0, tipoAporte.estado=1, dentro del mes)
     * - valoresCompensados           : siempre 0
     * - valoresPagados               : mismo valor que saldoCuentaIndividual
     */
    private ParticipeCesanteG43 construirCesante(
            String identificacion,
            String tipoIdentificacion,
            DetalleEjecucionReporte detalle,
            LocalDate fechaTerminacion,
            LocalDate fechaLiquidacion,
            LocalDateTime fechaInicioMes,
            LocalDateTime fechaFinMes) throws Throwable {

        ParticipeCesanteG43 cesante = new ParticipeCesanteG43();
        cesante.setIdentificacion(identificacion);
        cesante.setTipoIdentificacion(tipoIdentificacion);
        cesante.setDetalleEjecucion(detalle);
        cesante.setFechaTerminoRelacionLaboral(fechaTerminacion);
        cesante.setFechaLiquidacion(fechaLiquidacion);
        cesante.setValoresCompensados(0.0);

        // Buscar la entidad por identificacion
        Entidad entidad = null;
        try {
            entidad = entidadService.selectByNumeroIdentificacion(identificacion);
        } catch (Throwable e) {
            System.out.println("G43 - No se encontro entidad para identificacion: " + identificacion);
        }

        if (entidad != null) {
            Long codigoEntidad = entidad.getCodigo();

            // Numero imposiciones acumuladas personales: COUNT aportes > 0, tipo 9 y 11
            Long imposicionesPersonales = aporteService
                    .selectCountImposicionesPersonalesPorEntidad(codigoEntidad);
            cesante.setNumeroImposicionesPersonales(imposicionesPersonales != null ? imposicionesPersonales : 0L);

            // Numero imposiciones acumuladas patronales: COUNT aportes > 0, tipo 13 y 14
            Long imposicionesPatronales = aporteService
                    .selectCountImposicionesPatronalesPorEntidad(codigoEntidad);
            cesante.setNumeroImposicionesPatronales(imposicionesPatronales != null ? imposicionesPatronales : 0L);

            // Saldo cuenta individual: ABS(SUM aportes valor < 0, tipoAporte.estado=1, dentro del mes)
            Double sumaNegativa = aporteService
                    .selectSumaAportesNegativosMesPorEntidad(codigoEntidad, fechaInicioMes, fechaFinMes);
            double saldoCuenta = Math.abs(sumaNegativa != null ? sumaNegativa : 0.0);
            cesante.setSaldoCuentaIndividual(saldoCuenta);

            // Valores pagados = mismo valor que saldo cuenta individual
            cesante.setValoresPagados(saldoCuenta);

        } else {
            // Entidad no encontrada: poner 0 en todos los campos calculados
            cesante.setNumeroImposicionesPersonales(0L);
            cesante.setNumeroImposicionesPatronales(0L);
            cesante.setSaldoCuentaIndividual(0.0);
            cesante.setValoresPagados(0.0);
        }

        return cesante;
    }
}
