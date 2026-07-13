package com.saa.ejb.signature.service;

import jakarta.ejb.Local;

/**
 * Servicio para firma electrónica de documentos XML según estándares del SRI Ecuador.
 * Genera firmas PKCS#7 (XAdES-BES) compatibles con el formato requerido por el SRI.
 */
@Local
public interface SignatureService {
	
	/**
	 * Firma un documento XML con certificado digital PKCS#12.
	 * 
	 * @param xmlSinFirmar Contenido XML sin firmar
	 * @param pathCertificado Ruta al archivo .p12 del certificado digital
	 * @param passwordCertificado Contraseña del certificado
	 * @return XML firmado con formato compatible con SRI
	 * @throws Exception Si ocurre error en el proceso de firma
	 */
	String firmarXML(String xmlSinFirmar, String pathCertificado, String passwordCertificado) throws Exception;
	
	/**
	 * Firma un documento XML usando el certificado del facturador.
	 * 
	 * @param xmlSinFirmar Contenido XML sin firmar
	 * @param idFacturador ID del facturador (para ubicar su certificado)
	 * @return XML firmado con formato compatible con SRI
	 * @throws Exception Si ocurre error en el proceso de firma
	 */
	String firmarXMLFacturador(String xmlSinFirmar, Long idFacturador) throws Exception;
	
	/**
	 * Verifica si un XML está correctamente firmado.
	 * 
	 * @param xmlFirmado XML con firma digital
	 * @return true si la firma es válida, false en caso contrario
	 */
	boolean verificarFirma(String xmlFirmado);
}
