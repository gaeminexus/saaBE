package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de un anticipo de cliente (CBR.ANTC.ESTADO).
 *
 *         Ciclo de vida:
 *         INGRESADO --confirmación--> CONFIRMADO --anulación--> ANULADO
 *
 *         El anticipo de cliente se confirma en el mismo paso en que se
 *         registra (POST /antc/procesar): genera el asiento contable y
 *         acredita el saldo de anticipos del cliente. Recién entonces puede
 *         cruzarse contra facturas.
 */
public interface EstadoAnticipoCliente {

	/** Registrado, sin asiento ni saldo acreditado. */
	public static final int INGRESADO = 1;

	/** Con asiento contable y saldo disponible: se puede cruzar. */
	public static final int CONFIRMADO = 2;

	/** Anulado por el usuario (motivo obligatorio). */
	public static final int ANULADO = 3;

	/**
	 * Movimiento negativo histórico de un cruce anterior al 2026-08-20, cuando
	 * el cruce se registraba como una fila negativa en la propia tabla de
	 * anticipos. Se conserva como historial; las pantallas no lo leen porque el
	 * cruce vive ahora en CBR.APLC con FK al anticipo de origen.
	 * Ver docs/logica-negocio/pagos/MIGRACION-CRUCES-ANTICIPO.md
	 */
	public static final int MIGRADO = 4;

}
