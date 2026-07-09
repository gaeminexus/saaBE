package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.EjecucionReporte;

import jakarta.ejb.Local;

@Local
public interface EjecucionReporteDaoService extends EntityDao<EjecucionReporte> {

    /**
     * Recupera las ejecuciones de un mes y año específico.
     * @param mes  : Mes de presentación
     * @param anio : Año de presentación
     * @return     : Listado de ejecuciones
     * @throws Throwable : Excepcion
     */
    List<EjecucionReporte> selectByMesAnio(Long mes, Long anio) throws Throwable;
}
