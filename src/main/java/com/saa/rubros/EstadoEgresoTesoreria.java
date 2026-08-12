package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de un egreso de tesorería sin documento físico
 *         (TSR.EGRS.EGRSESTD).
 *
 *         Ciclo de vida:
 *         PENDIENTE_PAGO --pago confirmado--> PAGADO
 *              |                                 |
 *              +--> ANULADO (motivo)             +--reversión del pago--> PENDIENTE_PAGO
 *
 *         El egreso se paga a través del circuito de PagoProgramado (PGS.PGTR):
 *         al registrarlo se crea su pago, que aparece en el listado de pagos a
 *         realizar. La contabilidad se genera cuando el pago se confirma (o de
 *         inmediato si es débito automático).
 */
public interface EstadoEgresoTesoreria {

	/** Registrado, con su pago pendiente de ejecutarse. */
	public static final int PENDIENTE_PAGO = 1;

	/** El pago fue confirmado: tiene asiento y movimiento bancario. */
	public static final int PAGADO = 2;

	/** Anulado por el usuario (solo mientras estaba pendiente). */
	public static final int ANULADO = 3;

}
