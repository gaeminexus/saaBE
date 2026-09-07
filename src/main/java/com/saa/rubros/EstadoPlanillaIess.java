package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estado de RHH.PLIS.PLISESTD. No esta catalogado en SCP.PRBR/PDTR
 *         (es un flag simple, no un rubro con detalle) -- mismo criterio que
 *         {@link EstadoCajaChica}.
 *
 *         Ciclo: 1 -&gt; 2 -&gt; 3. El 4 es salida desde 1 o 2; desde 3 no se
 *         anula, se reversa (docs/logica-negocio/rhh/API-PLANILLA-IESS.md #4).
 */
public interface EstadoPlanillaIess {

	public static final int REGISTRADA = 1;
	public static final int CONCILIADA = 2;
	public static final int PAGADA = 3;
	public static final int ANULADA = 4;

}
