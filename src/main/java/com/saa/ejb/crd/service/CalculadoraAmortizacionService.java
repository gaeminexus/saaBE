package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.ejb.crd.service.dto.CuotaProyectada;
import com.saa.ejb.crd.service.dto.ParametrosAmortizacion;

import jakarta.ejb.Local;

/**
 * Núcleo de la matemática de amortización (francesa y alemana). Puro: sin {@code EntityManager},
 * sin leer ni escribir nada — recibe escalares y devuelve una tabla proyectada.
 *
 * Es la ÚNICA fórmula del sistema para generar una tabla de amortización nueva
 * ({@code PrestamoServiceImpl.generarAmortizacionFrancesa/Alemana} delegan acá) y para
 * simularla sin persistir. Ver PLAN-SIMULADORES-PRESTAMOS.md §4 decisión 3, §5 (auditoría de
 * los defectos D1-D10 que esta calculadora corrige) y §6 (arquitectura).
 *
 * @author Sistema SAA
 * @since 2026-08-25
 */
@Local
public interface CalculadoraAmortizacionService {

    /** 422 - Falta un campo obligatorio o tiene un valor no permitido */
    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    /** 422 - El monto es demasiado pequeño para el plazo pedido: alguna cuota quedaría en $0,00 */
    String ERR_MONTO_INSUFICIENTE = "MONTO_INSUFICIENTE_PARA_PLAZO";
    /** 422 - El plazo pedido supera el tope de seguridad */
    String ERR_PLAZO_EXCEDE_TOPE = "PLAZO_EXCEDE_TOPE";

    /**
     * Calcula la tabla de amortización completa (incluida la cuota 0 de gracia si se pide).
     *
     * @param params Monto, tasa anual, plazo, tipo de amortización, fecha de inicio, cuota 0 y
     *               los dos seguros por cuota
     * @return Una fila por cuota, en orden (0 si aplica, luego 1..plazo)
     * @throws Throwable Si algún parámetro es inválido
     */
    List<CuotaProyectada> calcular(ParametrosAmortizacion params) throws Throwable;
}
