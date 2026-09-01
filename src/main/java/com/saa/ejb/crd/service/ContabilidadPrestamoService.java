package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.ejb.crd.service.dto.ContextoPago;
import com.saa.ejb.crd.service.dto.MovimientoAporte;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.model.crd.EventoPrestamo;

import jakarta.ejb.Local;

/**
 * Hooks de contabilidad de los procesos de pago de préstamos (§9 de
 * ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md).
 *
 * Se invocan al FINAL de cada proceso, dentro de la MISMA transacción REQUIRED: cuando exista
 * una implementación real, un fallo del asiento (período inexistente o MAYORIZADO, debe ≠ haber)
 * revierte el pago completo. Ese es el comportamiento correcto para operaciones online, a
 * diferencia del lote petro.
 *
 * La implementación es {@code ContabilidadPrestamoServiceImpl} (reemplazó a
 * {@code ContabilidadPrestamoNoOpImpl}, borrada el 2026-08-31 — dos beans implementando este
 * mismo {@code @Local} dejan la inyección ambigua). Fase 1 de {@code PLAN-CIERRE-CONTABLE-TOTAL.md}:
 * solo {@link #contabilizarPagoConAportes} genera asiento; los otros cuatro siguen devolviendo
 * {@code null} a propósito hasta que el árbitro defina el discriminador de origen que evite
 * duplicar el asiento de los procesos que ya pasan por {@code CobroCreditoServiceImpl} (ver el
 * javadoc de {@code generarAsientoDefinitivo} ahí).
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Local
public interface ContabilidadPrestamoService {

    /**
     * @return Código del asiento creado, o null si la contabilidad no está activa.
     */
    Long contabilizarPagoCuota(ResultadoAplicacionPago resultado, ContextoPago ctx) throws Throwable;

    /**
     * @return Código del asiento creado, o null si la contabilidad no está activa.
     */
    Long contabilizarPagoConAportes(ResultadoAplicacionPago resultado, List<MovimientoAporte> movimientos,
            ContextoPago ctx) throws Throwable;

    /**
     * Asiento de re-bandeo (2026-08-31, Fase 3 — ver {@code ContabilizacionIndividualCreditoService
     * #lineasReclasificacionAbonoCapital}): reclasifica entre bandas el capital que sigue vivo
     * tras la re-amortización. NO lleva la plata del abono — esa la lleva CBCRASN2, el asiento
     * definitivo del cobro que originó este evento (único camino de aplicación hoy: el abono se
     * aplica siempre vía {@code CobroCreditoServiceImpl}, nunca directo — ver {@code
     * PrestamoRest#abonarCapital}). Que este método reciba {@code idEmpresa} y no un
     * {@code ContextoPago} completo es a propósito: este asiento no tiene el discriminador
     * {@code idCobroCredito} porque no lo necesita, no es la misma plata que CBCRASN2.
     *
     * @param idEmpresa Empresa contable — SIEMPRE derivada del lado del servidor (la cuenta
     *                  bancaria del cobro, vía {@code derivarEmpresaCobro}), nunca la que
     *                  mandaría un cliente (contrato API-EMPRESA-CONTABLE-CRD.md §2).
     * @return Código del asiento creado, o null si la contabilidad no está activa o si el
     *         abono no generó ninguna diferencia de banda que reclasificar.
     */
    Long contabilizarAbonoCapital(EventoPrestamo evento, Long idEmpresa) throws Throwable;

    /**
     * Cruce de valores de una precancelación con aportes CONSUMIDOS — SOLO cuando la llamada
     * es directa (2026-08-31, circuito de cobros con aportes, decisión del usuario). Si
     * {@code ctx.getIdCobroCredito()} viene con valor, la operación nació de
     * {@code CobroCreditoServiceImpl.procesarCobro}, que ya genera su propio asiento
     * (CBCRASN2) por la misma plata — este método debe devolver {@code null} sin generar nada.
     *
     * @param movimientos Aportes consumidos por la operación (puede venir vacío/null si la
     *                     precancelación fue 100% efectivo — ahí tampoco hay nada que cruzar)
     * @param ctx          Contexto de la operación; trae {@code idEmpresa} e
     *                     {@code idCobroCredito}
     * @return Código del asiento creado, o null si la contabilidad no está activa, si no hubo
     *         aportes consumidos, o si la llamada vino de CBCR.
     */
    Long contabilizarPrecancelacion(EventoPrestamo evento, List<MovimientoAporte> movimientos,
            ContextoPago ctx) throws Throwable;

    /**
     * @return Código del asiento creado, o null si la contabilidad no está activa.
     */
    Long contabilizarReverso(EventoPrestamo eventoAnulado) throws Throwable;
}
