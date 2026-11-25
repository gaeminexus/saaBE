package com.saa.ejb.rpr.service;

import com.saa.model.rpr.DetalleEjecucionReporte;

import jakarta.ejb.Local;

/**
 * C40 (23-nov-2026) — sustituciones de RPR.SSTG: lo que se presento a la Superintendencia y difiere de lo que genera el
 * SAA (D-154: operacion 71174 en junio y julio 2025). Se aplican dentro de la generacion de cada estructura, para que
 * regenerar un mes reproduzca lo presentado. No tocan la cartera ni la contabilidad.
 */
@Local
public interface SustitucionReporteGService {

    /**
     * Aplica las sustituciones activas del periodo y la estructura de {@code detalle} sobre lo que su generador acaba de
     * grabar. Devuelve cuantas filas agrega (0 si solo reemplaza). Falla si una sustitucion activa no encuentra su fila.
     */
    long aplicar(DetalleEjecucionReporte detalle) throws Throwable;
}
