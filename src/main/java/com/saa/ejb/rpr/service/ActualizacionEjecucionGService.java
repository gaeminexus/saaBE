package com.saa.ejb.rpr.service;

import com.saa.model.rpr.EjecucionReporte;

import jakarta.ejb.Local;

/**
 * C50: ata cada ejecución de las G40-G51 a la corrida de cartera vigente del mes y dice si sigue valiendo ({@link ReglaActualizacionG}).
 * Se calcula al consultar, sin triggers. Contrato: docs/logica-negocio/rpr/C50-G-ATADAS-A-LA-CORRIDA.md.
 */
@Local
public interface ActualizacionEjecucionGService {

    /** Guarda en la ejecución (en memoria, sin persistir) la corrida de cartera vigente del mes y el último asiento en 1.3/1.4/2.1 y el instante de generación. */
    void atar(EjecucionReporte ejrc) throws Throwable;

    /** Evalúa la ejecución contra la corrida vigente y los asientos de hoy. Con asiento posterior, el motivo trae su número y fecha. */
    ReglaActualizacionG.Resultado evaluar(EjecucionReporte ejrc) throws Throwable;

    /** Evalúa la última ejecución (código mayor) del mes; {@code null} si el mes no tiene ninguna. */
    ReglaActualizacionG.Resultado evaluarUltima(Long anio, Long mes) throws Throwable;
}
