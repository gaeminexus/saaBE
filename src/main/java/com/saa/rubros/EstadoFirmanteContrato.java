package com.saa.rubros;

/** Estado de un firmante del trámite de firma del contrato ({@code CRD.FRFR.FRFRESTD}). */
public interface EstadoFirmanteContrato {
	public static final int PENDIENTE = 1;
	public static final int FIRMADO = 2;
	public static final int RECHAZADO = 3;
}
