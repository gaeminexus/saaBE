package com.saa.rubros;

/**
 * CRD.POSG.POSGTPSG — tipo de seguro de un documento de póliza.
 * {@code docs/logica-negocio/crd/API-POLIZAS-SEGURO.md} §1.
 */
public interface TipoSeguro {

	public static final long DESGRAVAMEN = 1L;
	public static final long INCENDIO = 2L;
	public static final long PRENDARIO = 3L;

}
