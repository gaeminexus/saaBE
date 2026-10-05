package com.saa.rubros;

/**
 * CRD.MVIC.MVICCMPN — componente que afecta un movimiento del libro de intereses por cuota.
 */
public interface ComponenteMovimientoInteresCuota {

	/** Interés ordinario + interés vencido. */
	public static final long INTERES = 1L;
	public static final long MORA = 2L;
	/** Clasificación de capital por banda y tipo de cartera (tipos 7 y 8 únicamente). */
	public static final long CAPITAL = 3L;

}
