package com.saa.ejb.rpr.service;

import com.saa.model.rpr.DetalleEjecucionReporte;
import jakarta.ejb.Local;

@Local
public interface GeneracionG41Service {

    /**
     * Genera el reporte G41 (Partícipes Activos y Voluntarios).
     * - Busca entidades con idEstado = 1
     * - Si no hay ninguna → retorna 0 (G41 vacío, EJRD queda OK)
     * - Por cada entidad → busca su Participe y su Exter (por cedula)
     * - Mapea los datos a ParticipeActivoG41 con el detalleEjecucion
     * - Actualiza idEstado de la entidad a 10
     *
     * @param detalle : DetalleEjecucionReporte (EJRD) al que pertenecerán los registros
     * @return        : Cantidad de registros insertados en CG41
     * @throws Throwable : Excepcion
     */
    long generar(DetalleEjecucionReporte detalle) throws Throwable;
}
