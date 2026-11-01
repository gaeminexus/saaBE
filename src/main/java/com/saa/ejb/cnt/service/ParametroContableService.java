package com.saa.ejb.cnt.service;

import java.util.Map;

import jakarta.ejb.Local;

/**
 * Lectura de los parámetros contables del rubro {@link com.saa.rubros.Rubros#CNT_PARAMETROS_ESTADOS_FINANCIEROS}
 * (250), detalles en {@link com.saa.rubros.CntParametroEstadoFinanciero}. Único lugar que los consulta fuera de los
 * estados financieros oficiales: los controles de cierre (cnt) y el cierre de cartera (crd) leen de acá las cuentas de
 * nivel 1-2 que antes estaban escritas en el código (P2, P8 y P4-A de PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md).
 *
 * <p>Sin respaldo: si el detalle no existe, está inactivo o vacío, {@link #valor(int)} devuelve {@code null} y el
 * llamador decide (el cierre se detiene, el control avisa «No se pudo medir»).</p>
 */
@Local
public interface ParametroContableService {

	/** Detalles activos y no vacíos del rubro 250: PDTRALTR → PDTRVLRV (ya recortado). */
	Map<Integer, String> parametrosEstadosFinancieros();

	/** Valor del detalle {@code alternoDetalle} del rubro 250, o {@code null} si falta, está inactivo o vacío. */
	String valor(int alternoDetalle);

	/** La cuenta como prefijo de sus subcuentas: la misma con un punto al final si no lo trae ({@code 1.3} → {@code 1.3.}). */
	static String comoPrefijo(String cuenta) {
		return cuenta.endsWith(".") ? cuenta : cuenta + ".";
	}
}
