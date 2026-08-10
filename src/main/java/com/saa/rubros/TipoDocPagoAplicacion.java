package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Tipo de documento que paga/abona una factura en las tablas de
 *         aplicación de pago CBR.APLC (columna APLCTDPG) y PGS.APLP (APLPTDPG).
 *
 *         Nota: la Nota de Débito (5) se registra con montoAplicado NEGATIVO,
 *         porque aumenta el saldo pendiente de la factura en lugar de abonarlo.
 */
public interface TipoDocPagoAplicacion {

	/** Cobro/pago directo: transferencia, efectivo, cheque. */
	public static final int COBRO_DIRECTO = 1;

	/** Nota de Crédito que reduce el valor de la factura. */
	public static final int NOTA_CREDITO = 2;

	/** Retención (emitida al proveedor en CXP / recibida del cliente en CXC). */
	public static final int RETENCION = 3;

	/** Cruce con el saldo de anticipos del titular. */
	public static final int ANTICIPO = 4;

	/** Nota de Débito que aumenta el valor de la factura. Monto NEGATIVO. */
	public static final int NOTA_DEBITO = 5;

}
