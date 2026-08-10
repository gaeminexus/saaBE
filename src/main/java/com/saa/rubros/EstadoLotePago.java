package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de un lote de pagos (PGS.LTPG.LTPGESTD). Cada lote corresponde
 *         a un archivo de transferencias enviado a la entidad financiera.
 */
public interface EstadoLotePago {

	/** Archivo generado y enviado al banco, sin respuesta todavía. */
	public static final int GENERADO = 1;

	/** Ya se cargó y procesó el archivo de respuesta del banco. */
	public static final int RESPUESTA_PROCESADA = 2;

	/** Lote anulado (requiere motivo). */
	public static final int ANULADO = 3;

}
