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
 * La única implementación por ahora es {@code ContabilidadPrestamoNoOpImpl}: la contabilidad de
 * préstamos todavía no está activa y le faltan pre-requisitos (constante de módulo crédito en
 * {@code com.saa.rubros.ModuloSistema}, constantes en {@code com.saa.rubros.TipoAsientos} con
 * sus filas por empresa, y resolver el {@code idEmpresa} desde crd).
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
     * @return Código del asiento creado, o null si la contabilidad no está activa.
     */
    Long contabilizarAbonoCapital(EventoPrestamo evento) throws Throwable;

    /**
     * @return Código del asiento creado, o null si la contabilidad no está activa.
     */
    Long contabilizarPrecancelacion(EventoPrestamo evento) throws Throwable;

    /**
     * @return Código del asiento creado, o null si la contabilidad no está activa.
     */
    Long contabilizarReverso(EventoPrestamo eventoAnulado) throws Throwable;
}
