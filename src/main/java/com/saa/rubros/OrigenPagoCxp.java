package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Etiquetas de los documentos propios de CXP que pueden originar un pago
 *         (PGS.PGTR): factura de compra, egreso de tesorería o anticipo a proveedor.
 *
 *         Complementa a {@link OrigenPagoExterno}, que etiqueta los documentos que viven en
 *         OTROS módulos. Juntas forman el vocabulario completo del filtro {@code origen} de
 *         {@code GET /pgtr/porAprobar}: un pago de CXP siempre cae en una de estas tres, o en
 *         una de {@link OrigenPagoExterno} si {@code PGTRORGN} no es nulo.
 *
 *         A diferencia de {@link OrigenPagoExterno}, estas tres SÍ se resuelven en CXP: cada
 *         una corresponde exactamente a que {@code facturaCompra}/{@code egreso}/
 *         {@code anticipo} de {@code PagoProgramado} sea el que está no-nulo.
 */
public interface OrigenPagoCxp {

	/** El pago paga una factura de compra (PGS.FCTC). */
	String FACTURA_COMPRA = "FACTURA_COMPRA";

	/** El pago paga un egreso de tesorería sin documento físico (TSR.EGRS). */
	String EGRESO_TESORERIA = "EGRESO_TESORERIA";

	/** El pago paga un anticipo a proveedor (PGS.ANTP). */
	String ANTICIPO_PROVEEDOR = "ANTICIPO_PROVEEDOR";

}
