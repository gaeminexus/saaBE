package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de pago de una factura o liquidación
 *         (CBR.FCTR.FCTREPAG, PGS.FCTC.FCTCEPAG, CBR.LQCS.ESTADOPAGO).
 *
 *         Lo calcula y graba el backend a partir de las aplicaciones de pago
 *         activas del documento: la base de datos solo almacena el valor.
 */
public interface EstadoPagoFactura {

	/** Sin abonos: el saldo pendiente es el total del documento. */
	public static final int PENDIENTE = 1;

	/** Tiene abonos pero aún queda saldo pendiente. */
	public static final int PAGADA_PARCIAL = 2;

	/** El saldo pendiente llegó a cero. */
	public static final int PAGADA_TOTAL = 3;

}
