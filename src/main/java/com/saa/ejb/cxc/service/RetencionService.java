package com.saa.ejb.cxc.service;
import com.saa.basico.util.EntityService;
import com.saa.model.cxc.Retencion;
import jakarta.ejb.Local;
@Local
public interface RetencionService extends EntityService<Retencion> {

	/**
	 * Genera el XML de comprobante de retención electrónica según estándares del SRI v1.0.0.
	 * @param clave Clave de acceso de la retención
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @return Array con [mensaje, pathXMLRelativo, pathXMLAbsoluto]
	 * @throws Throwable
	 */
	String[] generarXMLRetencion(String clave, Long ambiente) throws Throwable;
	
	/**
	 * Autoriza el comprobante de retención electrónica ante el SRI.
	 * @param idFacturador ID del facturador
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @param conectaSRI Si conecta al SRI (1=SI, 0=NO)
	 * @param clave Clave de acceso
	 * @param codigoRetencion ID de la retención
	 * @param xml Contenido XML firmado
	 * @param destinatario Email del destinatario
	 * @param pathLogo Path del logo
	 * @return Mensaje con el resultado de la autorización
	 * @throws Throwable
	 */
	String autorizarRetencion(Long idFacturador, Long ambiente, Long conectaSRI, String clave, 
			Long codigoRetencion, String xml, String destinatario, String pathLogo) throws Throwable;

	/**
	 * Procesa un comprobante de retención completo: graba, genera XML, firma y autoriza ante el SRI.
	 * Configuración automática:
	 * - ambiente: 1 (PRUEBA) si es null
	 * - conectaSRI: 1 (SI) si es null
	 * - destinatario: se obtiene del mail del proveedor si es null
	 * - pathLogo: resources/logos/logo_aso.png si es null
	 * 
	 * @param retencion Objeto Retencion a procesar
	 * @param ambiente Ambiente (puede ser null para usar default)
	 * @param conectaSRI Si conecta al SRI (puede ser null para usar default)
	 * @param destinatario Email destinatario (puede ser null para usar del proveedor)
	 * @param pathLogo Path del logo (puede ser null para usar default)
	 * @return Map con el resultado del proceso completo
	 * @throws Throwable
	 */
	java.util.Map<String, Object> procesarRetencionCompleta(Retencion retencion,
			java.util.List<com.saa.model.cxc.DetalleRetencion> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;

}
