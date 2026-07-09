package com.saa.ejb.rpr.service;

import com.saa.model.rpr.EjecucionReporte;

import jakarta.ejb.Local;

@Local
public interface GeneracionReportesService {

    /**
     * Orquesta la generación de los reportes G40-G51 para un mes y año dado.
     * - Si ya existe una ejecución completa (todos OK) → lanza excepción informativa.
     * - Si existe una ejecución con fallos → re-ejecuta solo los Gs pendientes o con novedades.
     * - Si no existe → crea la cabecera EJRC + 12 detalles EJRD y ejecuta todos.
     *
     * @param mes    : Mes de presentación (1-12)
     * @param anio   : Año de presentación
     * @param usuario: Usuario que lanza el proceso
     * @return       : EjecucionReporte con el estado final
     * @throws Throwable : Excepcion
     */
    EjecucionReporte ejecutarGeneracion(Long mes, Long anio, String usuario) throws Throwable;
}
