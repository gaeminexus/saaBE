package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de un ingreso de tesorería sin documento físico
 *         (TSR.INGR.INGRESTD).
 *
 *         El ingreso se registra cuando el dinero ya entró a la cuenta: nace
 *         ACTIVO con su asiento y su movimiento bancario generados en el mismo
 *         paso. Si se registró mal, se anula (reversa el asiento y el
 *         movimiento).
 */
public interface EstadoIngresoTesoreria {

	/** Registrado y contabilizado. */
	public static final int ACTIVO = 1;

	/** Anulado: asiento y movimiento bancario reversados. */
	public static final int ANULADO = 2;

}
