package com.saa.rubros;

/**
 * Constantes de las reglas del otorgamiento de créditos (DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md,
 * partes B1, B2 y B5). Una sola fuente: la usan el evaluador de otorgamiento, el guardado del préstamo, el simulador de
 * novación y el proceso de mora (su tasa por defecto).
 */
public interface ParametroOtorgamiento {

	/** Tasa de interés anual (en %) de todo crédito nuevo que nace del otorgamiento. */
	double TASA_OTORGAMIENTO = 9.0;

	/** Capacidad de pago: fracción de la remuneración unificada que puede comprometerse en cuotas (50 %). */
	double PORCENTAJE_CAPACIDAD_PAGO = 0.50;

	/** Cupo: fracción de la cuenta individual que cubre el capital pendiente más el monto solicitado (100 %). */
	double PORCENTAJE_CUPO_CUENTA_INDIVIDUAL = 1.0;

	/** Tolerancia de redondeo al comparar valores en dólares. */
	double TOLERANCIA = 0.01;

	/** Código Petro del producto quirografario. */
	String CODIGO_PETRO_QUIROGRAFARIO = "PQ";

	/** Código Petro del producto emergente. */
	String CODIGO_PETRO_EMERGENTE = "PE";
}
