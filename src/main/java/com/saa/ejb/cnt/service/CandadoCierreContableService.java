package com.saa.ejb.cnt.service;

import java.util.List;

import com.saa.ejb.cnt.service.dto.ExcepcionCandado;
import com.saa.ejb.cnt.service.dto.ResultadoCandado;

import jakarta.ejb.Local;

/**
 * C51: candado de {@code POST /prdo/cerrar}. Con {@link #PROPIEDAD_CANDADO} encendida, el cierre contable exige (a) el semáforo
 * {@code /prdo/controlCierre} del período sin controles en ERROR (salvo excepción con motivo), (b) la última ejecución de las G40-G51 del mes COMPLETA y con la
 * validación previa sin errores, y (c) esa ejecución VIGENTE según el C50. (b) y (c) no admiten excepción. Apagado por defecto: el cierre no cambia, pero
 * la bitácora (CNT.HCPR) deja lo que habría bloqueado. Contrato: docs/logica-negocio/cnt/C51-CANDADO-CIERRE-CONTABLE.md.
 */
@Local
public interface CandadoCierreContableService {

    /** Propiedad del servidor: {@code true} enciende el candado. Apagada por defecto. Vive aquí, no en ModoCorteCartera (va también a producción sin los lotes de provisión). */
    String PROPIEDAD_CANDADO = "saa.cierres.candadoCierre";

    /** {@code true} si {@link #PROPIEDAD_CANDADO} está encendida. */
    static boolean activo() {
        return Boolean.parseBoolean(System.getProperty(PROPIEDAD_CANDADO, "false").trim());
    }

    /**
     * Qué bloquea hoy el cierre contable del período, con las excepciones recibidas (nulas o vacías = ninguna). Solo lee. Una excepción sin motivo, sin
     * código o dirigida a una condición de las G se rechaza con {@code IncomeException}.
     */
    ResultadoCandado consultar(Long idEmpresa, Long anio, Long mes, List<ExcepcionCandado> excepciones) throws Throwable;
}
