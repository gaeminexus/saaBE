package com.saa.rubros;

/**
 * Códigos alternos (CNT.PLNS.PLNSCDAL) de las plantillas contables de CXP.
 */
public interface PlantillasCxp {

	/**
	 * "CXP COMPRAS — CUENTAS DE AJUSTE". Plantilla de apoyo de los asientos de compra (factura, reembolso,
	 * nota de crédito y liquidación de compra): no define el asiento, solo las cuentas que éste no obtiene de los
	 * grupos de producto ni de PGS.TSRI. Líneas en {@link CxpLineaAsiento}. Una por empresa.
	 *
	 * <p>El alterno 38 es PROVISIONAL: hay que reservarlo en {@code docs/logica-negocio/REGISTRO-RESERVAS-EQUIPOS.md}
	 * §2c antes de escribir la semilla (los alternos 1-37 los usa CRD).</p>
	 */
	public static final int COMPRAS = 38;
}
