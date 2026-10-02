package com.saa.rubros;

/**
 * CRD.POSG.POSGESTD — ciclo de un documento de póliza.
 * {@code docs/logica-negocio/crd/DISENO-POLIZAS-SEGURO-PRESTAMOS.md} §5.1:
 * <pre>
 * 1 LISTADO_ENVIADO ─▶ 2 DOCUMENTO_REGISTRADO ─▶ 3 DISTRIBUIDO ─▶ 4 LIBERADO_A_PAGO
 *         │                     │                       │
 *         └──────────── 5 ANULADO ◀─────────────────────┘
 * </pre>
 */
public interface EstadoDocumentoSeguro {

	public static final long LISTADO_ENVIADO = 1L;
	public static final long DOCUMENTO_REGISTRADO = 2L;
	public static final long DISTRIBUIDO = 3L;
	public static final long LIBERADO_A_PAGO = 4L;
	public static final long ANULADO = 5L;

}
