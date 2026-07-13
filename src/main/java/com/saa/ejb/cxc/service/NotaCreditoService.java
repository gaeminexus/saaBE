package com.saa.ejb.cxc.service;
import com.saa.basico.util.EntityService;
import com.saa.model.cxc.NotaCredito;
import jakarta.ejb.Local;
@Local
public interface NotaCreditoService extends EntityService<NotaCredito> {

	/**
	 * Genera el XML de nota de crédito electrónica según estándares del SRI.
	 * @param clave Clave de acceso de la nota de crédito
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @return Array con [mensaje, pathXMLRelativo, pathXMLAbsoluto]
	 * @throws Throwable
	 */
	String[] generarXMLNotaCredito(String clave, Long ambiente) throws Throwable;
	
	/**
	 * Autoriza la nota de crédito electrónica ante el SRI.
	 * @param idFacturador ID del facturador
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @param conectaSRI Si conecta al SRI (1=SI, 0=NO)
	 * @param clave Clave de acceso
	 * @param codigoNotaCredito ID de la nota de crédito
	 * @param xml Contenido XML firmado
	 * @param destinatario Email del destinatario
	 * @param pathLogo Path del logo
	 * @return Mensaje con el resultado de la autorización
	 * @throws Throwable
	 */
	String autorizarNotaCredito(Long idFacturador, Long ambiente, Long conectaSRI, String clave, 
			Long codigoNotaCredito, String xml, String destinatario, String pathLogo) throws Throwable;

	/**
	 * Procesa una nota de crédito completa: graba, genera XML, firma y autoriza ante el SRI.
	 * Configuración automática:
	 * - ambiente: 1 (PRUEBA) si es null
	 * - conectaSRI: 1 (SI) si es null
	 * - destinatario: se obtiene del mail del comprador si es null
	 * - pathLogo: resources/logos/logo_aso.png si es null
	 * 
	 * @param notaCredito Objeto NotaCredito a procesar
	 * @param ambiente Ambiente (puede ser null para usar default)
	 * @param conectaSRI Si conecta al SRI (puede ser null para usar default)
	 * @param destinatario Email destinatario (puede ser null para usar del comprador)
	 * @param pathLogo Path del logo (puede ser null para usar default)
	 * @return Map con el resultado del proceso completo
	 * @throws Throwable
	 */
	java.util.Map<String, Object> procesarNotaCreditoCompleta(NotaCredito notaCredito,
			java.util.List<com.saa.model.cxc.DetalleNotaCredito> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;

}
