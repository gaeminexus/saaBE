package com.saa.rubros;

/**
 * Catálogo semántico de las líneas (DTPLAXL1) de la plantilla contable de COMPRAS de CXP
 * ({@link PlantillasCxp#COMPRAS}). Los asientos de compra de {@code AsientoContableServiceImpl} arman sus
 * líneas de gasto, IVA y proveedor desde los grupos de producto, {@code PGS.TSRI} y la cuenta del titular; esta
 * plantilla solo guarda las cuentas que no salen de ahí y que antes estaban escritas en el código
 * (P5, PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md).
 */
public interface CxpLineaAsiento {

	/**
	 * Cuenta de ajuste por diferencia de redondeo del SRI entre el {@code importeTotal} de la cabecera del XML y
	 * la suma de las líneas DEBE de un documento de compra. Hoy {@code 4.8.90.90.35}. El lado (DEBE o HABER) lo
	 * decide el código según el signo de la diferencia; el movimiento de la línea es indiferente. DTPLAXL2 = 0.
	 */
	public static final int REDONDEO_SRI = 1;

	/**
	 * Cuenta de gasto para lo que no tiene producto clasificado: el detalle sin producto de una liquidación de compra y
	 * la base de una nota de débito de compra (P11, 2026-11-01). Antes se tomaba la cuenta del grupo de producto de
	 * MENOR código de la empresa. La cuenta la decide contabilidad (semilla pendiente). DTPLAXL2 = 0.
	 */
	public static final int GASTO_SIN_CLASIFICAR = 2;
}
