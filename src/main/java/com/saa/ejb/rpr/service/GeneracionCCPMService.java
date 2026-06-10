package com.saa.ejb.rpr.service;

import com.saa.model.rpr.EjecucionReporteCartera;

import jakarta.ejb.Local;

@Local
public interface GeneracionCCPMService {

    /**
     * Genera los registros del CCPM (CreditoCuotasPrestamosMensual - PRP.CCPM).
     * Lógica similar al G48 pero con campos adicionales: valorDesgravamen y valorIncendio.
     *
     * @param ejecucion EjecucionReporteCartera al que pertenece esta generación
     * @return cantidad de operaciones procesadas
     */
    long generar(EjecucionReporteCartera ejecucion) throws Throwable;
}
