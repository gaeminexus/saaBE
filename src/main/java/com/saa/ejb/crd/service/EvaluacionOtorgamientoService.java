package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.EvaluacionOtorgamiento;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * Reglas del otorgamiento de créditos (DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md, B1 y B2): capacidad de
 * pago (cuota nueva + cuotas vigentes ≤ 50 % de la remuneración unificada) y cupo (capital pendiente de los vigentes +
 * monto ≤ 100 % de la cuenta individual). Es UN solo cálculo, sin tabla, que usan guardar, generar la tabla, aprobar, el
 * simulador de novación y la pantalla, para que nunca den números distintos.
 *
 * <p>No aplica a lo que no nace del otorgamiento: la carga Petro y la migración (no pasan por aquí) ni la
 * reestructuración de un préstamo en mora.</p>
 */
@Local
public interface EvaluacionOtorgamientoService {

    /** Mensaje de la regla de la tasa (B5). */
    String MSG_TASA = "La tasa de interés de los créditos es 9 %";

    /** Mensaje cuando el partícipe no tiene remuneración unificada (B2). */
    String MSG_SIN_REMUNERACION = "Registre la remuneración unificada del partícipe en Actualización de datos → Datos del partícipe";

    /**
     * Evalúa capacidad y cupo.
     *
     * @param idEntidad          partícipe
     * @param monto              monto del crédito (en una novación incluye el valor de sustitución)
     * @param cuotaNueva         cuota TOTAL de la primera cuota de la tabla; {@code null} = todavía no hay cuota (no se
     *                           evalúa la capacidad)
     * @param idPrestamoActual   el préstamo que se evalúa (se excluye de las sumas), o {@code null}
     * @param idPrestamoOriginal el préstamo que se nova (se excluye de las sumas: se cancela con el nuevo), o {@code null}
     */
    EvaluacionOtorgamiento evaluar(Long idEntidad, Double monto, Double cuotaNueva, Long idPrestamoActual,
            Long idPrestamoOriginal) throws Throwable;

    /**
     * Igual que la anterior, con el código Petro del producto del préstamo nuevo: el cupo de la cuenta individual solo
     * se evalúa si es PQ/PE (con otro código, {@code cupoOk = null}: no aplica; una cadena VACÍA significa «producto
     * sin código Petro», p. ej. CENAPRO, y tampoco aplica); con {@code null} (producto desconocido) se evalúa. El cupo suma solo el capital de los vigentes PQ/PE ({@code saldoCapitalCupo}).
     */
    EvaluacionOtorgamiento evaluar(Long idEntidad, Double monto, Double cuotaNueva, Long idPrestamoActual,
            Long idPrestamoOriginal, String codigoPetroNuevo) throws Throwable;

    /** Evalúa un préstamo guardado: monto, cuota 1 de su tabla (si ya la tiene) y su original. Siempre informa. */
    EvaluacionOtorgamiento evaluarPrestamo(Long idPrestamo) throws Throwable;

    /**
     * {@code true} si el préstamo está sujeto a las reglas: nace del otorgamiento (alta o estado 6/1) y no es una
     * reestructuración.
     *
     * @param existente el préstamo ya guardado, o {@code null} si es un alta
     */
    boolean aplicaReglas(Prestamo prestamo, Prestamo existente) throws Throwable;

    /**
     * Guardado de un préstamo 6/1 del otorgamiento: la tasa es 9 % (nula o 0 → 9; otra → error) y el cupo (error si lo
     * supera). No evalúa capacidad: todavía no hay cuota.
     *
     * @throws com.saa.basico.util.IncomeException con el motivo (la capa REST lo responde 422)
     */
    void validarAlGuardar(Prestamo prestamo, Prestamo existente) throws Throwable;

    /**
     * Aprobación: sin remuneración, capacidad y cupo bloquean. Lee la cuota 1 de la tabla ya generada.
     *
     * @throws com.saa.basico.util.IncomeException con el motivo (la capa REST lo responde 422)
     */
    void validarAlAprobar(Prestamo prestamo) throws Throwable;

    /**
     * Cuota TOTAL vigente de un préstamo: la de su primera cuota no pagada (sin la cuota 0); si no le quedan pendientes,
     * la de su última cuota; 0 si no tiene tabla.
     */
    double cuotaVigenteDe(Long idPrestamo) throws Throwable;
}
