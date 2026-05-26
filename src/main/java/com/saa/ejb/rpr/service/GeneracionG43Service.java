package com.saa.ejb.rpr.service;

import com.saa.model.rpr.DetalleEjecucionReporte;
import jakarta.ejb.Local;

@Local
public interface GeneracionG43Service {

    /**
     * Genera los registros del G43 (ParticipeCesanteG43 - RPR.CG43).
     *
     * Lógica:
     *  1. Obtener el G42 del mes de ejecución actual (CG42 del EJRD G42 de este EJRC).
     *  2. Calcular el mes/año anterior y buscar el EJRC previo.
     *     - Si existe EJRC previo y tiene EJRD G42 con registros → usar CG42 de ese EJRD
     *       como "universo del mes anterior".
     *     - Si NO existe EJRC previo (primera vez) → usar HistoricoG42 como "universo anterior".
     *  3. Entidades que estaban en el universo anterior pero NO aparecen en el G42 actual
     *     → son partícipes cesantes → se insertan en CG43.
     *
     * @param detalle DetalleEjecucionReporte del G43 en curso
     * @return cantidad de registros insertados en CG43
     */
    long generar(DetalleEjecucionReporte detalle) throws Throwable;
}
