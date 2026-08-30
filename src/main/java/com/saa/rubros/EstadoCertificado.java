package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Estados de un certificado de partícipe emitido (CRD.CRTF.CRTFESTD).
 *         Un certificado anulado conserva su número: el hueco en la serie queda
 *         documentado con su motivo, nunca silencioso.
 */
public interface EstadoCertificado {

	int EMITIDO = 1;
	int ANULADO = 2;

}
