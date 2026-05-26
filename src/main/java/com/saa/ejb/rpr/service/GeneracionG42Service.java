package com.saa.ejb.rpr.service;

import com.saa.model.rpr.DetalleEjecucionReporte;

import jakarta.ejb.Local;

@Local
public interface GeneracionG42Service {

    /**
     * Genera los registros del G42 (SaldoCuentaG42 - RPR.CG42).
     * Lógica:
     *  - Ejecuta 4 SELECTs con SUM agrupados por entidad en la BD
     *  - Por cada entidad con valor > 0: INSERT o UPDATE en CG42
     *
     * @param detalle DetalleEjecucionReporte al que pertenece esta generación
     * @return cantidad de entidades procesadas
     */
    long generar(DetalleEjecucionReporte detalle) throws Throwable;
}
