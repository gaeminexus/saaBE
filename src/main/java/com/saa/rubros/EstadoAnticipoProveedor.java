package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de un anticipo a proveedor (PGS.ANTP.ANTPESTD).
 *
 *         Ciclo de vida:
 *         INGRESADO --pago confirmado--> CONFIRMADO
 *              |                             |
 *              +--> ANULADO (motivo)         +--reversión del pago--> INGRESADO
 *
 *         El anticipo se paga a través del circuito de PagoProgramado (PGS.PGTR):
 *         al registrarlo se crea su pago, que aparece en el listado de pagos a
 *         realizar. La contabilidad (asiento de anticipo, movimiento bancario y
 *         saldo de anticipos del proveedor) se genera cuando el pago se confirma,
 *         o de inmediato si es débito automático.
 */
public interface EstadoAnticipoProveedor {

	/** Registrado, con su pago pendiente de ejecutarse. Sin asiento ni saldo. */
	public static final int INGRESADO = 1;

	/** El pago fue confirmado: tiene asiento, movimiento bancario y saldo. */
	public static final int CONFIRMADO = 2;

	/** Anulado por el usuario (motivo obligatorio). Ver ANULACION-ANTICIPOS.md */
	public static final int ANULADO = 3;

	/**
	 * Movimiento negativo histórico de un cruce anterior al 2026-08-20, cuando
	 * el cruce se registraba como una fila negativa en la propia tabla de
	 * anticipos. Se conserva como historial; las pantallas no lo leen porque el
	 * cruce vive ahora en PGS.APLP con FK al anticipo de origen.
	 * Ver docs/logica-negocio/pagos/MIGRACION-CRUCES-ANTICIPO.md
	 */
	public static final int MIGRADO = 4;

}
