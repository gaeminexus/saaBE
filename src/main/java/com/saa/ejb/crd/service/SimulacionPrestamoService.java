package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.ParametrosAmortizacion;
import com.saa.ejb.crd.service.dto.ResultadoSimulacionCreditoNuevo;
import com.saa.ejb.crd.service.dto.ResultadoSimulacionReestructuracion;
import com.saa.ejb.crd.service.dto.SolicitudReestructuracion;

import jakarta.ejb.Local;

/**
 * Simuladores de préstamo nuevo y de reestructuración (PLAN-SIMULADORES-PRESTAMOS.md §1, fase 2).
 * Ninguno de los dos escribe nada (decisión 8): ni {@code CRD.PRST} ni {@code CRD.DTPR}.
 *
 * Los dos reusan {@link CalculadoraAmortizacionService}: reestructurar es la misma calculadora
 * sembrada con el saldo actual del préstamo en vez de un monto pedido (§6).
 *
 * @author Sistema SAA
 * @since 2026-08-25
 */
@Local
public interface SimulacionPrestamoService {

    /** 422 - La calculadora solo soporta un único período de gracia (0 o 1) */
    String ERR_GRACIA_NO_SOPORTADA = "GRACIA_NO_SOPORTADA";

    /**
     * Tabla de amortización de un préstamo que todavía no existe.
     *
     * @param params Monto, tasa, plazo, tipo de amortización, fecha de inicio, cuota 0 y seguros
     * @return Tabla proyectada + totales de cabecera
     * @throws Throwable Si algún parámetro es inválido
     */
    ResultadoSimulacionCreditoNuevo simularCreditoNuevo(ParametrosAmortizacion params) throws Throwable;

    /**
     * Cómo quedaría un préstamo existente si se reestructura, combinando las cuatro palancas de
     * la decisión 2 del plan: ampliar/reducir plazo, capitalizar mora e interés vencido, cambiar
     * la tasa, y período de gracia.
     *
     * @param solicitud Préstamo y las cuatro palancas
     * @return Comparativa antes/después + tabla proyectada nueva
     * @throws Throwable Si el préstamo no existe, está en estado terminal, o algún parámetro es inválido
     */
    ResultadoSimulacionReestructuracion simularReestructuracion(SolicitudReestructuracion solicitud) throws Throwable;
}
