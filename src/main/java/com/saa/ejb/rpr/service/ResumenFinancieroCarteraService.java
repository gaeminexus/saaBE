package com.saa.ejb.rpr.service;

import com.saa.ejb.rpr.service.dto.ResumenFinancieroCartera;

import jakarta.ejb.Local;

/**
 * C27-E — Resumen Financiero de Informes Mensuales comparado con el balance. Ver
 * {@link ResumenFinancieroCartera}.
 */
@Local
public interface ResumenFinancieroCarteraService {

    /**
     * Arma el resumen de una ejecución guardada (no recalcula la cartera: lee RPR.CCPB y RPR.CPRM).
     *
     * @param codigoEjecucion : RPR.EJCC
     * @param idEmpresa       : Empresa de los asientos; null = todas
     */
    ResumenFinancieroCartera armar(Long codigoEjecucion, Long idEmpresa) throws Throwable;
}
