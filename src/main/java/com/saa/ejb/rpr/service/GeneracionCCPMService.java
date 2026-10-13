package com.saa.ejb.rpr.service;

import com.saa.model.rpr.EjecucionReporteCartera;

import jakarta.ejb.Local;

@Local
public interface GeneracionCCPMService {

    /**
     * Genera los registros del CCPM (CreditoCuotasPrestamosMensual - PRP.CCPM).
     *
     * <p>2026-10-13 (CCPM adopta las bandas del cierre,
     * {@code DISENO-CCPM-BANDAS-COMO-EL-CIERRE.md}): el universo y la clasificación por banda
     * ya no son propios de CCPM — son el mismo universo del cierre de cartera
     * ({@code CierreCarteraDaoService#selectCuotasPendientesPorPrestamo}) clasificado con
     * {@code ContabilizacionIndividualCreditoService#tipoCarteraYDias} y
     * {@code ClasificadorBandaService}. Intereses, mora, seguros, calificación, provisión,
     * cuenta individual, fechas y montoSolicitado siguen con su lógica de siempre.</p>
     *
     * @param ejecucion EjecucionReporteCartera al que pertenece esta generación
     * @param idEmpresa Empresa a generar; si es nulo, se resuelve sola cuando hay una única
     *                  empresa con configuración de bandas vigente — ver la implementación
     * @return cantidad de operaciones procesadas
     */
    long generar(EjecucionReporteCartera ejecucion, Long idEmpresa) throws Throwable;
}
