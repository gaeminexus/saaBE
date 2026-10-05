package com.saa.rubros;

/**
 * Estado de seguros de un documento de compra (FCTC/NTDC/NTCC) — docs/logica-negocio/cxp/
 * API-DOCUMENTOS-SEGUROS-CXP.md §2. No confundir con {@link EstadoDocumentoSeguro}, el ciclo de
 * {@code CRD.POSG} (la póliza en crédito): éste vive del lado de CxP, en el propio documento.
 */
public interface EstadoSeguroDocumentoCxp {

	/** No es un documento de seguros. Valor por defecto. */
	public static final long NO_ES_SEGURO = 0L;

	/**
	 * De seguros, BLOQUEADO: una factura no se puede pagar; una ND está contabilizada pero no
	 * aplicada a la factura; una NC está aplicada (como cualquier NC) y sólo informa a crédito.
	 */
	public static final long BLOQUEADO = 1L;

	/** De seguros, LIBERADO por crédito: factura pagable, ND aplicada, NC igual que antes. */
	public static final long LIBERADO = 2L;

}
