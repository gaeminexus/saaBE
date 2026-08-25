package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de una devolucion de aportes a un participe (CRD.DVAP.DVAPESTD).
 *
 *         Ciclo de vida:
 *         REGISTRADA --orden de pago generada--> EN_PAGO --pago confirmado--> PAGADA
 *              |                                    |
 *              +--> ANULADA (motivo)                +--pago rechazado o reversado--> RECHAZADA
 *
 *         Los aportes NEGATIVOS de CRD.APRT se generan al REGISTRAR, antes de que el
 *         dinero salga del banco: el saldo del participe baja de inmediato. Si el pago
 *         termina RECHAZADA, el reconciliador inserta los contra-movimientos POSITIVOS
 *         (nunca borra ni edita la fila negativa: CRD.APRT es append-only para los
 *         reportes) y el saldo vuelve a su valor previo.
 *
 *         El estado NO lo mueve CXP: lo actualiza el reconciliador de CRD
 *         (DevolucionAporteService.sincronizarPagos) leyendo el estado real del
 *         PagoProgramado. CXP no puede nombrar a CRD.
 */
public interface EstadoDevolucionAporte {

	/** Registrada, todavia sin orden de pago asociada. */
	public static final int REGISTRADA = 1;

	/** Con orden de pago generada en CXP, esperando que el banco la ejecute. */
	public static final int EN_PAGO = 2;

	/** El pago quedo confirmado: el dinero salio y el asiento se genero. */
	public static final int PAGADA = 3;

	/** El pago fue rechazado o reversado: se generaron los contra-movimientos. */
	public static final int RECHAZADA = 4;

	/** Anulada por el usuario antes de pagarse (requiere motivo). */
	public static final int ANULADA = 5;

}
