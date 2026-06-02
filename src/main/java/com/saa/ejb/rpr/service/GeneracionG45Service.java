package com.saa.ejb.rpr.service;

import com.saa.model.rpr.DetalleEjecucionReporte;
import jakarta.ejb.Local;

@Local
public interface GeneracionG45Service {

    /**
     * Genera los registros del G45 (NuevoParticipeG45 - RPR.CG45).
     *
     * Lógica (debe ejecutarse DESPUÉS del G46):
     *  - Lee los registros de CG46 del mismo EJRC (los nuevos préstamos del mes).
     *  - Por cada entidad incluida en el G46, cuenta cuántos préstamos vigentes (2),
     *    en mora (8) o de plazo vencido (11) tiene en total.
     *  - Solo se incluye en G45 aquellas entidades que tengan exactamente 1 préstamo
     *    en esos estados (es decir, únicamente el que fue incluido en el G46).
     *    Las entidades con más de 1 se omiten (ya tenían préstamo previo).
     *  - Por cada entidad calificada, enriquece con:
     *      - Exter       → genero, estadoCivil, profesion, provincia, canton, fechaNacimiento
     *      - Direccion   → parroquia (primera dirección, parroquia.nombre)
     *      - PerfilEconomico → patrimonio (patrimonioNeto), origenIngresos (origenOtrosIngresos)
     *      - Participe   → tipoParticipe (tipoParticipante.nombre), actividadEconomica (ingresoAdicionalActividad)
     *  - INSERT en CG45 por cada partícipe calificado.
     *
     * @param detalle DetalleEjecucionReporte al que pertenece esta generación
     * @return cantidad de registros insertados en CG45
     */
    long generar(DetalleEjecucionReporte detalle) throws Throwable;
}
