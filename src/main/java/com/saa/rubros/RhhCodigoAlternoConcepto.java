package com.saa.rubros;

/**
 * Codigos alternos (RHH.CPNM.CPNMALTR) de conceptos de nomina que el codigo necesita ubicar por
 * nombre. Se resuelven SIEMPRE con la empresa ({@code ConceptoNominaDaoService.selectByCodigoAlterno}),
 * nunca contra el CPNMCDGO, que cambia de una empresa a otra.
 */
public interface RhhCodigoAlternoConcepto {

	/** «Vacaciones pagadas»: la novedad que crea la aprobacion de una solicitud de vacaciones. */
	long VACACIONES_PAGADAS = 12L;

}
