package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.CreditoJubiladosMensual;

import jakarta.ejb.Local;

@Local
public interface CreditoJubiladosMensualService extends EntityService<CreditoJubiladosMensual> {

    /**
     * Retorna todos los registros CJBM que pertenecen a una ejecución específica.
     * @param codigoEjecucion Código de la EjecucionReporteCartera
     * @return Lista de CreditoJubiladosMensual
     */
    List<CreditoJubiladosMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable;
}
