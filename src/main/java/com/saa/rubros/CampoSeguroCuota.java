package com.saa.rubros;

/**
 * CRD.PSCT.PSCTCMPO — qué campo de la cuota tocó un documento de póliza. El incendio y el
 * prendario comparten el mismo campo de la cuota ({@code DTPRVLSI}): no hay un tercer valor.
 * {@code docs/logica-negocio/crd/sql/306_DDL_POLIZAS_SEGURO.sql}.
 */
public interface CampoSeguroCuota {

	/** DTPRDSGR — desgravamen. */
	public static final long DESGRAVAMEN = 1L;
	/** DTPRVLSI — incendio o prendario (ambos usan el mismo campo). */
	public static final long SEGURO = 2L;

}
