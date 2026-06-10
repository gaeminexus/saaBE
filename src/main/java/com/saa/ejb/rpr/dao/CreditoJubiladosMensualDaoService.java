package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.CreditoJubiladosMensual;
import jakarta.ejb.Local;

@Local
public interface CreditoJubiladosMensualDaoService extends EntityDao<CreditoJubiladosMensual> {

    /**
     * Retorna todos los registros CJBM que pertenecen a una ejecución específica.
     *
     * @param codigoEjecucion Código de la EjecucionReporteCartera (RPR.EJCC)
     * @return Lista de CreditoJubiladosMensual de la ejecución indicada
     */
    List<CreditoJubiladosMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable;

    /** Elimina todos los registros CJBM de una ejecución específica. */
    int deleteByEjecucion(Long codigoEjecucion) throws Throwable;
}
