package com.saa.ejb.cxc.service;

import jakarta.ejb.Local;

/**
 * Servicio para envío de correos electrónicos relacionados con facturación electrónica.
 */
@Local
public interface EmailFacturaService {

	/**
	 * Envía por correo electrónico la factura autorizada con el XML adjunto.
	 *
	 * @param destinatario     Email del destinatario (cliente)
	 * @param numeroFactura    Número de la factura (ej: 001-001-000000001)
	 * @param clave            Clave de acceso de la factura
	 * @param razonSocialEmisor Razón social del facturador
	 * @param xmlAutorizado    Contenido XML autorizado para adjuntar
	 * @param pdfBytes         Bytes del PDF RIDE (puede ser null)
	 */
	void enviarFacturaAutorizada(String destinatario, String numeroFactura, String clave,
			String razonSocialEmisor, String xmlAutorizado, byte[] pdfBytes) throws Exception;
}
