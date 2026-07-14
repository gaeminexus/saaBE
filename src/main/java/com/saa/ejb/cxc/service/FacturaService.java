package com.saa.ejb.cxc.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cxc.Factura;

import jakarta.ejb.Local;

@Local
public interface FacturaService extends EntityService<Factura> {

	/**
	 * Genera el XML de factura electrónica según estándares del SRI.
	 * @param clave Clave de acceso de la factura
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @return Array con [mensaje, pathXMLRelativo, pathXMLAbsoluto]
	 * @throws Throwable
	 */
	String[] generarXMLFactura(String clave, Long ambiente) throws Throwable;
	
	/**
	 * Autoriza la factura electrónica ante el SRI.
	 * @param idFacturador ID del facturador
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @param conectaSRI Si conecta al SRI (1=SI, 0=NO)
	 * @param clave Clave de acceso
	 * @param codigoFactura ID de la factura
	 * @param subsidio Valor del subsidio
	 * @param xml Contenido XML firmado
	 * @param destinatario Email del destinatario
	 * @param pathLogo Path del logo
	 * @return Mensaje con el resultado de la autorización
	 * @throws Throwable
	 */
	String autorizarFactura(Long idFacturador, Long ambiente, Long conectaSRI, String clave, 
			Long codigoFactura, Double subsidio, String xml, String destinatario, String pathLogo) throws Throwable;
	
	/**
	 * Procesa una factura completa: graba, genera XML, firma y autoriza ante el SRI.
	 * Este es el método principal que debe llamar el frontend.
	 * 
	 * @param factura Objeto Factura con todos los datos
	 * @param detalles Lista de detalles de la factura
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @param conectaSRI Si conecta al SRI (1=SI, 0=NO)
	 * @param destinatario Email del destinatario (opcional)
	 * @param pathLogo Path del logo (opcional)
	 * @return Mapa con el resultado del proceso completo
	 * @throws Throwable
	 */
	java.util.Map<String, Object> procesarFacturaCompleta(com.saa.model.cxc.Factura factura, 
			java.util.List<com.saa.model.cxc.DetalleFactura> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;
	
	/**
	 * MÉTODO DE PRUEBA: Envía un XML correcto al SRI para probar la comunicación
	 */
	String probarEnvioXMLCorrecto(String xmlCorrecto) throws Exception;

}
