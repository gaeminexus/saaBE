package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.EjecucionReporteCartera;

import jakarta.ejb.Local;

@Local
public interface EjecucionReporteCarteraService extends EntityService<EjecucionReporteCartera> {

    /**
     * Busca ejecuciones de reportes de cartera por mes y año.
     * @param mes Mes (1-12)
     * @param anio Año
     * @return Lista de ejecuciones encontradas
     */
    List<EjecucionReporteCartera> selectByMesAnio(Long mes, Long anio) throws Throwable;
}
