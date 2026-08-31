package com.saa.rubros;

/**
 * @author Sistema SAA
 *         Estado de un pago mensual de pensión complementaria (CRD.PGPC.PGPCESTD).
 *
 *         Mismo ciclo de vida que {@link EstadoDevolucionAporte} y por el mismo motivo: es
 *         dinero saliendo hacia un tercero (el jubilado) a través de CXP, con el mismo circuito
 *         de aprobación de tesorería.
 *
 *         REGISTRADA --orden de pago generada--&gt; EN_PAGO --pago confirmado--&gt; PAGADA
 *              |                                    |
 *              +--&gt; ANULADA (motivo)                +--pago rechazado o reversado--&gt; RECHAZADA
 *
 *         El movimiento NEGATIVO de CRD.APRT (tipo 23, pensión complementaria) se genera al
 *         REGISTRAR, antes de que el dinero salga del banco: el saldo del jubilado baja de
 *         inmediato. Si el pago termina RECHAZADA, el reconciliador inserta el contra-movimiento
 *         POSITIVO (nunca borra ni edita la fila negativa: CRD.APRT es append-only).
 *
 *         El estado NO lo mueve CXP: lo actualiza el reconciliador de CRD leyendo el estado
 *         real del PagoProgramado. CXP no puede nombrar a CRD.
 */
public interface EstadoPagoPensionComplementaria {

	/** Registrado, todavía sin orden de pago asociada. */
	public static final int REGISTRADA = 1;

	/** Con orden de pago generada en CXP, esperando que el banco la ejecute. */
	public static final int EN_PAGO = 2;

	/** El pago quedó confirmado: el dinero salió. */
	public static final int PAGADA = 3;

	/** El pago fue rechazado o reversado: se generó el contra-movimiento. */
	public static final int RECHAZADA = 4;

	/** Anulado por el usuario antes de pagarse (requiere motivo). */
	public static final int ANULADA = 5;

}
