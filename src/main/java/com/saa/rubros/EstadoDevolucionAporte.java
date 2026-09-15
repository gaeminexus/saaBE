package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de una devolucion de aportes a un participe (CRD.DVAP.DVAPESTD).
 *
 *         Ciclo de vida:
 *         REGISTRADA --orden de pago generada--> EN_PAGO --pago confirmado--> PAGADA
 *              |
 *              +--> ANULADA (motivo)
 *
 *         Los aportes NEGATIVOS de CRD.APRT se generan al REGISTRAR, antes de que el
 *         dinero salga del banco: el saldo del participe baja de inmediato.
 *
 *         RECHAZADA es HISTORICO desde el contrato de reemision de pago (2026-09-15,
 *         docs/logica-negocio/crd/API-REEMITIR-PAGO-DEVOLUCION.md #4): una orden rechazada
 *         o anulada por tesoreria YA NO mueve la devolucion a este estado ni genera
 *         contra-movimientos en automatico. La devolucion se queda EN_PAGO, a la espera de
 *         que el operador reemita el pago (POST /dvap/{id}/reemitirPago) o anule la
 *         devolucion explicitamente (POST /dvap/anular/{id}, que ahora si permite anular una
 *         PAGADA cuando su orden quedo rechazada o anulada). El valor 4 se conserva para leer
 *         devoluciones que ya habian llegado a RECHAZADA con el mecanismo viejo.
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

	/**
	 * Historico (ver el javadoc de la interfaz): hasta el 2026-09-15 el reconciliador lo
	 * asignaba cuando el pago era rechazado o reversado, generando los contra-movimientos.
	 * Ya no se asigna desde ese mecanismo; se conserva para leer datos anteriores.
	 */
	public static final int RECHAZADA = 4;

	/** Anulada por el usuario (requiere motivo): antes de pagarse, o como reversion completa
	 *  explicita de una PAGADA cuya orden quedo rechazada/anulada en tesoreria. */
	public static final int ANULADA = 5;

}
