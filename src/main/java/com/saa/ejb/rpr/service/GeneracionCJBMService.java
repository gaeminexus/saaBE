package com.saa.ejb.rpr.service;

import com.saa.model.rpr.EjecucionReporteCartera;

import jakarta.ejb.Local;

@Local
public interface GeneracionCJBMService {

    /**
     * Genera los registros del CJBM (CreditoJubiladosMensual - PRP.CJBM).
     * Lógica idéntica al G44 - calcula datos de jubilados.
     *
     * @param ejecucion EjecucionReporteCartera al que pertenece esta generación
     * @return cantidad de jubilados procesados
     */
    long generar(EjecucionReporteCartera ejecucion) throws Throwable;
}
