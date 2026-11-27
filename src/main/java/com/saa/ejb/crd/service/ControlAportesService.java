package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.ejb.cnt.service.dto.ControlCierre;
import com.saa.ejb.crd.service.dto.ResultadoControlAportes;

import jakarta.ejb.Local;

/**
 * C53: control previo al cierre de cartera de los aportes del mes. Reglas en {@link ReglaControlAportes}. Corre en {@code /cierrecartera/previsualizar} (advertencia),
 * en {@code /prdo/controlCierre} (controles {@code APORTES_SIN_ASIENTO} y {@code APORTES_21_VS_MODULO}, ERROR) y en {@code /cierrecartera/ejecutar} solo con
 * {@link #PROPIEDAD_BLOQUEO} encendida. Contrato: docs/logica-negocio/crd/C53-APORTES-SIN-ASIENTO.md.
 */
@Local
public interface ControlAportesService {

    /** Propiedad: si esta encendida ({@code true}), aportes sin asiento o 2.1 distinta del modulo bloquea el ejecutar del cierre de cartera. Apagada por defecto. */
    String PROPIEDAD_BLOQUEO = "saa.cartera.bloqueoAportesSinAsiento";

    /** {@code true} si {@link #PROPIEDAD_BLOQUEO} esta encendida. */
    static boolean bloqueoActivo() {
        return Boolean.parseBoolean(System.getProperty(PROPIEDAD_BLOQUEO, "false").trim());
    }

    /** Controles A y B del mes (solo lectura). */
    ResultadoControlAportes consultar(Long idEmpresa, Long anio, Long mes) throws Throwable;

    /** Texto del control (A: lista y total; B: diferencias por cuenta) o {@code null} si todo esta en orden. Para la advertencia y el bloqueo del cierre de cartera. */
    String textoControl(Long idEmpresa, Long anio, Long mes) throws Throwable;

    /** Controles del semaforo: {@code APORTES_SIN_ASIENTO} y {@code APORTES_21_VS_MODULO}, ERROR si hay diferencia. */
    List<ControlCierre> controlesCierre(Long idEmpresa, Long anio, Long mes);
}
