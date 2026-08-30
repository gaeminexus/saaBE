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
 *
 *         <p><b>POR_APROBAR (0), agregado el 2026-08-27</b> (punto 14, ver
 *         docs/logica-negocio/pagos/PLAN-REDISENO-APROBACION-PAGOS.md): la solicitud nace
 *         sin cuenta bancaria ni forma de pago cuando el {@code registrar*} correspondiente
 *         recibe {@code idCuentaBancariaOrigen=null}. Tesorería la aprueba eligiendo la
 *         cuenta y la forma de pago, y ahí recién entra al ciclo de arriba:</p>
 *         <pre>
 *         POR_APROBAR(0) --aprobar, transferencia--> REGISTRADO(1)
 *         POR_APROBAR(0) --aprobar, cheque o débito automático--> CONFIRMADO(3)
 *         </pre>
 *         <p>PGTRESTD no tiene CHECK ni FK a catálogo (verificado 2026-08-27): este estado
 *         vive solo aquí, no hace falta ningún cambio de base para agregarlo.</p>
 */
public interface EstadoPagoProgramado {

	/**
	 * Solicitud de pago sin cuenta bancaria ni forma de pago asignada todavía.
	 * Nace así cuando {@code registrar*} recibe la cuenta en null. Tesorería la
	 * aprueba con {@code POST /pgtr/aprobar}, que asigna cuenta y forma de pago y
	 * la mueve a REGISTRADO o CONFIRMADO según corresponda.
	 */
	public static final int POR_APROBAR = 0;

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
