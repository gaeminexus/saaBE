package com.saa.rubros;

/**
 * CRD.POSG.POSGCLSE — clase de un documento de póliza: factura (la madre) o una de sus
 * notas. {@code docs/logica-negocio/crd/API-POLIZAS-SEGURO.md} §1.
 */
public interface ClaseDocumentoSeguro {

	public static final long FACTURA = 1L;
	public static final long NOTA_DEBITO = 2L;
	public static final long NOTA_CREDITO = 3L;

}
