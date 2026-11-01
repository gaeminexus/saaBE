package com.saa.rubros;

/**
 * Destino que el operador elige para el SOBRANTE de un partícipe en una carga Petro/ARCH (CRD.DSSB.DSSBTPDS), B8
 * (DISENO-DESTINO-SOBRANTE-DSSB.md). El sobrante es lo que el proceso automático no pudo aplicar del descuento
 * del partícipe; antes quedaba como novedad bloqueante y se repartía a mano por monto (AVPC).
 */
public interface DestinoSobrante {

	/**
	 * El motor aplica el sobrante al préstamo elegido ({@code DSSB.PRSTCDGO}) o, si es nulo, a la cuota pendiente más
	 * antigua entre los préstamos VIGENTE y EN_MORA del partícipe (el plazo vencido no entra, decisión del 2026-09-01).
	 */
	int SIGUIENTE_CUOTA = 1;

	/** Aporte: se reparte por devengo en el tipo de aporte elegido ({@code DSSB.TPAPCDGO}: 9 jubilación, 11 cesantía). */
	int APORTE = 2;

	/**
	 * Devolución al partícipe: no mueve dinero en la carga; lleva el valor a «socios por pagar» (línea
	 * {@link CrdLineaAsiento#DEVOLUCION_DESCUENTO_PARTICIPE} de la plantilla 21) y crea una orden en la bandeja de
	 * Tesorería ({@link OrigenPagoExterno#CRD_DEVOLUCION_DESCUENTO}).
	 */
	int DEVOLUCION = 3;
}
