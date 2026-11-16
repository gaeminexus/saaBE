package com.saa.ejb.crd.service;

import java.time.LocalDate;

import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * C31 (16-nov-2026): la garantía de un préstamo en cuentas de orden sale por el MONTO OTORGADO
 * ({@code PRSTMNSL}, el mismo valor con que la entrega la registra) cuando el préstamo termina, y vuelve a
 * entrar si la terminación se revierte. Generaliza C30 (novación) a todos los caminos de terminación.
 *
 * <p>Un solo punto de entrada, {@link #sincronizar}, que los procesos llaman después de cambiar el estado
 * del préstamo. Es idempotente: compara el estado con el neto de las bajas ya contabilizadas para ese
 * préstamo (marca {@code [C31 PRST id]} en la glosa de las líneas) y solo actúa si no coinciden.</p>
 */
@Local
public interface GarantiaOrdenPrestamoService {

    /**
     * Deja las cuentas de orden de la garantía del préstamo de acuerdo con su estado actual:
     * <ul>
     *   <li>CANCELADO (3) o CANCELADO_ANTICIPADO (4) sin baja vigente: asiento de baja (plantilla 26) por el
     *       monto otorgado (pagaré) más la suma asegurada del bien si la tiene (prendario/hipotecario).</li>
     *   <li>Vivo (2, 8, 10, 11) con una baja vigente (la terminación se revirtió): se anula el asiento de
     *       baja (o se reversa en el primer período abierto si el suyo está cerrado).</li>
     *   <li>Cualquier otro estado o caso: no hace nada. La novación (5) la resuelve el asiento de la
     *       sustitución (C30); la reestructuración (13) queda pendiente de decisión.</li>
     * </ul>
     *
     * @param prestamo  préstamo ya con su estado nuevo
     * @param idEmpresa empresa contable; si es null se toma la del asiento de entrega o la única empresa con
     *                  la plantilla 26
     * @param fecha     fecha contable de la terminación (la del cobro / afectación); null = hoy
     * @param usuario   usuario que registra o anula
     * @param causa     texto corto para la glosa (camino de terminación)
     * @return código del asiento generado o anulado; null si no hubo nada que hacer
     */
    Long sincronizar(Prestamo prestamo, Long idEmpresa, LocalDate fecha, String usuario, String causa)
            throws Throwable;
}
