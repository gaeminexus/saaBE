package com.saa.ejb.rpr.service;

import com.saa.model.rpr.DetalleEjecucionReporte;
import jakarta.ejb.Local;

@Local
public interface GeneracionG45Service {

    /**
     * Genera los registros del G45 (NuevoParticipeG45 - RPR.CG45).
     *
     * Lógica:
     *  - Lee los registros de CG41 del mismo EJRC (los nuevos partícipes que G41 procesó,
     *    ya que G41 cambia idEstado 1→10 antes de que corra G45).
     *  - Por cada partícipe activo del G41, busca en Entidad por identificacion para
     *    obtener el codigoEntidad y luego enriquece con:
     *      - Exter       → genero, estadoCivil, profesion, provincia, canton, fechaNacimiento
     *      - Direccion   → parroquia (primera dirección, parroquia.nombre)
     *      - PerfilEconomico → patrimonio (patrimonioNeto), origenIngresos (origenOtrosIngresos)
     *      - Participe   → tipoParticipe (tipoParticipante.nombre), actividadEconomica (ingresoAdicionalActividad)
     *  - INSERT en CG45 por cada nuevo partícipe.
     *
     * @param detalle DetalleEjecucionReporte al que pertenece esta generación
     * @return cantidad de registros insertados en CG45
     */
    long generar(DetalleEjecucionReporte detalle) throws Throwable;
}
