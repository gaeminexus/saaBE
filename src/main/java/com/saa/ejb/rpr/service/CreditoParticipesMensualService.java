package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.CreditoParticipesMensual;

import jakarta.ejb.Local;

@Local
public interface CreditoParticipesMensualService extends EntityService<CreditoParticipesMensual> {

    /**
     * Retorna todos los registros CPRM que pertenecen a una ejecución específica.
     * @param codigoEjecucion Código de la EjecucionReporteCartera
     * @return Lista de CreditoParticipesMensual
     */
    List<CreditoParticipesMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable;
}

