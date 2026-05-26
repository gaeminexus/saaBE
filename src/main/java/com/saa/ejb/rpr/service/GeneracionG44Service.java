package com.saa.ejb.rpr.service;

import com.saa.model.rpr.DetalleEjecucionReporte;
import jakarta.ejb.Local;

@Local
public interface GeneracionG44Service {

    /**
     * Genera los registros del G44 (ParticipeJubiladoG44 - RPR.CG44).
     *
     * Lógica:
     *  - Obtiene todas las entidades con idEstado = 30 (jubilados)
     *  - Por cada entidad:
     *      - imposicionesAcumuladas : COUNT de aportes con tipoAporte.codigo IN (9, 11)
     *      - valorPension           : valorPagar de ValorPagoPensionComplementaria por entidad
     *      - valorNetoRecibir       : mismo valorPagar (mismo origen que valorPension)
     *      - saldoCuenta            : SUM de aportes.valor con tipoAporte.codigo = 23
     *      - jubilacionIess         : FIJO = "S"
     *  - INSERT en CG44 por cada entidad jubilada
     *
     * @param detalle DetalleEjecucionReporte al que pertenece esta generación
     * @return cantidad de registros insertados en CG44
     */
    long generar(DetalleEjecucionReporte detalle) throws Throwable;
}
