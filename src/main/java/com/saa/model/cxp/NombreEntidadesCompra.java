package com.saa.model.cxp;

/**
 * Interface que contiene constantes con nombres de las Entidades del módulo CXP (Compras).
 */
public interface NombreEntidadesCompra {

	// Factura Compra
	String FACTURA_COMPRA = "FacturaCompra";
	String DETALLE_FACTURA_COMPRA = "DetalleFacturaCompra";
	String PATH_FACTURA_COMPRA = "PathFacturaCompra";
	String FORMA_PAGO_FACTURA_COMPRA = "FormaPagoFacturaCompra";

	// Liquidación Compra Compra
	String LIQUIDACION_COMPRA_COMPRA = "LiquidacionCompraCompra";
	String DETALLE_LIQUIDACION_COMPRA_COMPRA = "DetalleLiquidacionCompraCompra";
	String PATH_LIQUIDACION_COMPRA_COMPRA = "PathLiquidacionCompraCompra";
	String FORMA_PAGO_LIQUIDACION_COMPRA_COMPRA = "FormaPagoLiquidacionCompraCompra";

	// Nota de Crédito Compra
	String NOTA_CREDITO_COMPRA = "NotaCreditoCompra";
	String DETALLE_NOTA_CREDITO_COMPRA = "DetalleNotaCreditoCompra";
	String PATH_NOTA_CREDITO_COMPRA = "PathNotaCreditoCompra";

	// Nota de Débito Compra
	String NOTA_DEBITO_COMPRA = "NotaDebitoCompra";
	String DETALLE_NOTA_DEBITO_COMPRA = "DetalleNotaDebitoCompra";
	String PATH_NOTA_DEBITO_COMPRA = "PathNotaDebitoCompra";

	// Retención Compra
	String RETENCION_COMPRA = "RetencionCompra";
	String DETALLE_RETENCION_COMPRA = "DetalleRetencionCompra";
	String PATH_RETENCION_COMPRA = "PathRetencionCompra";

	// Retención Compra V2
	String RETENCION_COMPRA_V2 = "RetencionCompraV2";
	String DETALLE_RETENCION_COMPRA_V2 = "DetalleRetencionCompraV2";

	// Carga archivo TXT SRI
	String CARGA_ARCHIVO_TXT  = "CargaArchivoTxt";
	String DETALLE_CARGA_TXT  = "DetalleCargaTxt";

	// Documento único CXP (seguimiento por claveAcceso)
	String DOCUMENTO_CXP      = "DocumentoCxp";

	// Negociaciones con Proveedores
	String NEGOCIACION_PROVEEDOR    = "NegociacionProveedor";
	String FORMA_PAGO_NEGOCIACION   = "FormaPagoNegociacion";
	String PAGO_NEGOCIACION         = "PagoNegociacion";
	String ADENDUM_NEGOCIACION      = "AdendumNegociacion";
	String PATH_NEGOCIACION         = "PathNegociacion";
}
