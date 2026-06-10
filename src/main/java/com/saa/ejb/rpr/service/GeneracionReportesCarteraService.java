package com.saa.ejb.rpr.service;

import com.saa.model.rpr.EjecucionReporteCartera;
import jakarta.ejb.Local;

@Local
public interface GeneracionReportesCarteraService {

    /**
     * Ejecuta la generación de los 3 reportes de cartera: CPRM, CJBM y CCPM.
     * 
     * @param mes Mes de ejecución (1-12)
     * @param anio Año de ejecución
     * @param usuario Usuario que ejecuta la generación
     * @return EjecucionReporteCartera creado con el resultado
     */
    EjecucionReporteCartera ejecutarGeneracion(Long mes, Long anio, String usuario) throws Throwable;

    /**
     * Elimina la ejecución existente (y sus hijos CCPM, CJBM, CPRM) y vuelve a generar.
     */
    EjecucionReporteCartera regenerarGeneracion(Long codigoEjecucion, String usuario) throws Throwable;
}
