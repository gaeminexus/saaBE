package com.saa.ejb.cnt.service;

import java.util.List;

import com.saa.ejb.cnt.service.dto.ControlCierre;
import com.saa.ejb.cnt.service.dto.ResultadoRegularizacionRedondeos;
import com.saa.ejb.cnt.service.dto.SaldoContrarioCuenta;

import jakarta.ejb.Local;

/**
 * C52 (D-150, D-222): saldo contrario a la naturaleza POR CUENTA, sobre CNT.DTAS / ASNT / PLNN (no usa el motor de balances: va tambien a produccion sin la C19b).
 * Reglas en {@link ReglaSaldoContrario}. Contrato y aceptacion: docs/logica-negocio/cnt/C52-SALDO-CONTRARIO-POR-CUENTA.md.
 */
@Local
public interface SaldoContrarioCuentaService {

    /** Cuentas de movimiento con saldo contrario al corte del mes (solo lectura), con el asiento sugerido (no se graba). */
    List<SaldoContrarioCuenta> saldosContrarios(Long idEmpresa, Long anio, Long mes) throws Throwable;

    /** Control {@code SALDO_CONTRARIO_CUENTA} del semaforo {@code /prdo/controlCierre}: ERROR con la lista (acotada) si hay alguna cuenta. */
    List<ControlCierre> controlesCierre(Long idEmpresa, Long anio, Long mes);

    /**
     * Unica correccion automatica: las cuentas de redondeo de la plantilla (alterno 40, papel 1) con saldo acreedor van a otros ingresos (papel 2).
     * Asiento del mes con fecha del ultimo dia; idempotente; con el periodo contable cerrado no se graba. {@code simular = true} solo calcula.
     */
    ResultadoRegularizacionRedondeos regularizarRedondeos(Long idEmpresa, Long anio, Long mes, String usuario, boolean simular) throws Throwable;
}
