package com.saa.ejb.rpr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.EjecucionReporte;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface EjecucionReporteService extends EntityService<EjecucionReporte> {

    /**
     * Recupera las ejecuciones de un mes y año específico.
     * @param mes  : Mes de presentación
     * @param anio : Año de presentación
     * @return     : Listado de ejecuciones
     * @throws Throwable : Excepcion
     */
    List<EjecucionReporte> selectByMesAnio(Long mes, Long anio) throws Throwable;
}
