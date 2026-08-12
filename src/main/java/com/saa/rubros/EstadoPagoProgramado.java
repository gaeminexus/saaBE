package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de un pago a proveedor por transferencia (PGS.PGTR.PGTRESTD).
 *
 *         Ciclo de vida:
 *         REGISTRADO --selección--> EN_ARCHIVO --respuesta banco--> CONFIRMADO
 *                                       |
 *                                       +--> RECHAZADO (queda en seguimiento)
 *
 *         La contabilidad, la aplicación de pago y el movimiento bancario se
 *         generan ÚNICAMENTE cuando el pago llega a CONFIRMADO: un pago que el
 *         banco no ejecutó no debe afectar el saldo bancario ni la conciliación.
 *
 *         Los pagos por débito automático (PGS.PGTR.PGTRDBAT=1) ya fueron
 *         ejecutados por el banco cuando se registran, así que se saltan los
 *         estados intermedios: nacen CONFIRMADOS y, si se reversan, quedan
 *         ANULADOS (no RECHAZADOS: no hay nada que reprogramar).
 */
public interface EstadoPagoProgramado {

	/** Registrado, pendiente de ser seleccionado para un lote. */
	public static final int REGISTRADO = 1;

	/** Incluido en un lote/archivo enviado a la entidad financiera. */
	public static final int EN_ARCHIVO = 2;

	/** El banco confirmó la transferencia: genera contabilidad y abona la factura. */
	public static final int CONFIRMADO = 3;

	/** El banco no ejecutó la transferencia. Queda en seguimiento. */
	public static final int RECHAZADO = 4;

	/** Anulado por el usuario (requiere motivo). */
	public static final int ANULADO = 5;

}
