package com.saa.ejb.cxc.service;

import jakarta.ejb.Local;

/**
 * Servicio para envío de correos electrónicos relacionados con facturación electrónica.
 */
@Local
public interface EmailFacturaService {

	/**
	 * Envía por correo electrónico el comprobante electrónico autorizado con el XML adjunto.
	 *
	 * @param destinatario      Email del destinatario
	 * @param numeroDocumento   Número del documento (ej: 001-001-000000001)
	 * @param clave             Clave de acceso
	 * @param razonSocialEmisor Razón social del facturador
	 * @param tipoDocumento     Nombre del documento para el asunto/cuerpo del email
	 *                          (ej: "Factura", "Nota de Crédito", "Nota de Débito",
	 *                               "Retención", "Liquidación de Compra")
	 * @param xmlAutorizado     Contenido XML autorizado para adjuntar
	 * @param pdfBytes          Bytes del PDF RIDE (puede ser null)
	 */
	void enviarFacturaAutorizada(String destinatario, String numeroDocumento, String clave,
			String razonSocialEmisor, String tipoDocumento, String xmlAutorizado, byte[] pdfBytes) throws Exception;
}