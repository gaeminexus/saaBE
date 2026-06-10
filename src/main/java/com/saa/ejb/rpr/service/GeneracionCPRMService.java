package com.saa.ejb.rpr.service;

import com.saa.model.rpr.EjecucionReporteCartera;

import jakarta.ejb.Local;

@Local
public interface GeneracionCPRMService {

    /**
     * Genera los registros del CPRM (CreditoParticipesMensual - PRP.CPRM).
     * Lógica idéntica al G42 - calcula aportes y saldos por entidad.
     *
     * @param ejecucion EjecucionReporteCartera al que pertenece esta generación
     * @return cantidad de entidades procesadas
     */
    long generar(EjecucionReporteCartera ejecucion) throws Throwable;
}
