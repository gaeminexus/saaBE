package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.CreditoParticipesMensual;
import com.saa.model.rpr.EjecucionReporteCartera;

import jakarta.ejb.Local;

@Local
public interface CreditoParticipesMensualDaoService extends EntityDao<CreditoParticipesMensual> {

    /**
     * Busca un registro de CPRM por entidad, tipo de aporte y ejecución.
     * Permite hacer UPDATE si ya existe o INSERT si no existe.
     *
     * @param codigoEntidad   Código de la entidad (CRD.ENTD)
     * @param codigoTipoAporte Código del tipo de aporte (CRD.TPAP)
     * @param ejecucion       Ejecución de reporte de cartera (RPR.EJCC)
     * @return CreditoParticipesMensual encontrado o null
     */
    CreditoParticipesMensual selectByEntidadTipoAporteYEjecucion(
            Long codigoEntidad, Long codigoTipoAporte, EjecucionReporteCartera ejecucion) throws Throwable;

    /**
     * Retorna todos los registros CPRM que pertenecen a una ejecución específica.
     *
     * @param codigoEjecucion Código de la EjecucionReporteCartera (RPR.EJCC)
     * @return Lista de CreditoParticipesMensual de la ejecución indicada
     */
    List<CreditoParticipesMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable;

    /** Elimina todos los registros CPRM de una ejecución específica. */
    int deleteByEjecucion(Long codigoEjecucion) throws Throwable;
}