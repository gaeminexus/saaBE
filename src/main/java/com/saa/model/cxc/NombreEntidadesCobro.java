/**
 * Copyright (c) 2010 Compuseg C�a. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la informaci�n confidencial y patentada de   Compuseg C�a. Ltda. ( "Informaci�n Confidencial"). 
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad con los t�rminos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.cxc;

/**
 * <p>Interface que contiene constantes con 
 *  Nombre de las Entidades del Sistema.</p>
 */
public interface NombreEntidadesCobro {
	
	String COMPOSICION_CUOTA_INICIAL_COBRO = "ComposicionCuotaInicialCobro";
	String CUOTA_X_FINANCIACION_COBRO = "CuotaXFinanciacionCobro";
	String DETALLE_DOCUMENTO_COBRO = "DetalleDocumentoCobro";
	String DOCUMENTO_COBRO = "DocumentoCobro";
	String FINANCIACION_X_DOCUMENTO_COBRO = "FinanciacionXDocumentoCobro"; 
	String GRUPO_PRODUCTO_COBRO = "GrupoProductoCobro";
	String PRODUCTO_COBRO = "ProductoCobro";
	String IMPUESTO_X_GRUPO_COBRO = "ImpuestoXGrupoCobro";
	String PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO = "PagosArbitrariosXFinanciacionCobro";
	String RESUMEN_VALOR_DOCUMENTO_COBRO = "ResumenValorDocumentoCobro";
	String TEMP_COMPOSICION_CUOTA_INICIAL_COBRO = "TempComposicionCuotaInicialCobro";
	String TEMP_CUOTA_X_FINANCIACION_COBRO = "TempCuotaXFinanciacionCobro";
	String TEMP_DETALLE_DOCUMENTO_COBRO = "TempDetalleDocumentoCobro";
	String TEMP_DOCUMENTO_COBRO = "TempDocumentoCobro";
	String TEMP_FINANCIACION_X_DOCUMENTO_COBRO = "TempFinanciacionXDocumentoCobro"; 
	String TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO = "TempPagosArbitrariosXFinanciacionCobro";
	String TEMP_RESUMEN_VALOR_DOCUMENTO_COBRO = "TempResumenValorDocumentoCobro";
	String TEMP_VALOR_IMPUESTO_DETALLE_COBRO = "TempValorImpuestoDetalleCobro";
	String TEMP_VALOR_IMPUESTO_DOCUMENTO_COBRO = "TempValorImpuestoDocumentoCobro";
	String VALOR_IMPUESTO_DETALLE_COBRO = "ValorImpuestoDetalleCobro";
	String VALOR_IMPUESTO_DOCUMENTO_COBRO = "ValorImpuestoDocumentoCobro";
	String FACTURADOR = "Facturador";
	String ESTABLECIMIENTO = "Establecimiento";
	String PUNTO_EMISION = "PuntoEmision";
	String NUMERACION_PUNTO_EMISION = "NumeracionPuntoEmision";
	String LSRI = "Lsri";
	String TSRI = "Tsri";
	String FACTURA = "Factura";
	String DETALLE_FACTURA = "DetalleFactura";
	String PATH_FACTURA = "PathFactura";
	String FORMA_PAGO_FACTURA = "FormaPagoFactura";
	String ANTICIPO_CLIENTE   = "AnticipoCliente";
	
	// Liquidaciones de Compras y Servicios
	String LIQUIDACION_COMPRA = "LiquidacionCompra";
	String DETALLE_LIQUIDACION_COMPRA = "DetalleLiquidacionCompra";
	String PATH_LIQUIDACION_COMPRA = "PathLiquidacionCompra";
	String FORMA_PAGO_LIQUIDACION = "FormaPagoLiquidacion";
	
	// Notas de Crédito
	String NOTA_CREDITO = "NotaCredito";
	String DETALLE_NOTA_CREDITO = "DetalleNotaCredito";
	String PATH_NOTA_CREDITO = "PathNotaCredito";
	
	// Notas de Débito
	String NOTA_DEBITO = "NotaDebito";
	String DETALLE_NOTA_DEBITO = "DetalleNotaDebito";
	String PATH_NOTA_DEBITO = "PathNotaDebito";
	
	// Retenciones
	String RETENCION = "Retencion";
	String DETALLE_RETENCION = "DetalleRetencion";
	String PATH_RETENCION = "PathRetencion";
	
	// Retenciones V2
	String RETENCION_V2 = "RetencionV2";
	String DETALLE_RETENCION_V2 = "DetalleRetencionV2";
	String PATH_RETENCION_V2 = "PathRetencionV2";
}