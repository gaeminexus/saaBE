package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de una aplicación de pago (CBR.APLC.APLCESTD / PGS.APLP.APLPESTD).
 *
 *         Al pasar a REVERSADO los triggers de base de datos recalculan el
 *         estado de pago de la factura y devuelven el saldo del anticipo.
 */
public interface EstadoAplicacionPago {

	/** Aplicación vigente: descuenta del saldo de la factura. */
	public static final int ACTIVO = 1;

	/** Aplicación reversada: ya no descuenta del saldo de la factura. */
	public static final int REVERSADO = 2;

}
