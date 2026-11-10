package com.saa.rubros;

/**
 * Estado del trámite de firma electrónica del contrato de adhesión ({@code CRD.FRCN.FRCNESTD}). Estado técnico de proceso:
 * constantes planas, sin rubro. Vivo = {@link #PENDIENTE_ENVIO} o {@link #ENVIADO} (solo uno por contrato).
 */
public interface EstadoFirmaContrato {
	public static final int PENDIENTE_ENVIO = 1;
	public static final int ENVIADO = 2;
	public static final int FIRMADO = 3;
	public static final int RECHAZADO = 4;
	public static final int VENCIDO = 5;
	public static final int ANULADO = 6;
}
