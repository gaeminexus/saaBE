package com.saa.rubros;

/**
 * CRD.PSPR.PSPRNVDD — por qué un préstamo está dentro de un documento de póliza: venía en el
 * listado original, entró después por una nota de débito (inclusión), o salió por una nota de
 * crédito (exclusión). {@code docs/logica-negocio/crd/API-POLIZAS-SEGURO.md} §1.
 */
public interface NovedadPrestamoSeguro {

	public static final long ORIGINAL = 1L;
	public static final long INCLUSION = 2L;
	public static final long EXCLUSION = 3L;

}
