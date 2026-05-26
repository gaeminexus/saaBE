package com.saa.ejb.rpr.service;

import com.saa.model.rpr.DetalleEjecucionReporte;
import jakarta.ejb.Local;

@Local
public interface GeneracionG40Service {

    /**
     * Genera el reporte G40 (Información General del Fondo).
     * - Si el registro IGFN tiene estado = 2 (Modificado) → copia sus datos a CG40
     *   y resetea el estado de IGFN a 1 (Sin cambios).
     * - Si tiene estado = 1 (Sin cambios) → no inserta nada, retorna 0.
     * En ambos casos el EJRD queda en OK.
     *
     * @param detalle : DetalleEjecucionReporte (EJRD) al que pertenecerá el registro CG40
     * @return        : Cantidad de registros insertados (0 o 1)
     * @throws Throwable : Excepcion
     */
    long generar(DetalleEjecucionReporte detalle) throws Throwable;
}
