package com.saa.ejb.cxc.service;
import com.saa.basico.util.EntityService;
import com.saa.model.cxc.RetencionV2;
import jakarta.ejb.Local;
@Local
public interface RetencionV2Service extends EntityService<RetencionV2> {

	/**
	 * Genera el XML de comprobante de retención electrónica según estándares del SRI v2.0.0.
	 * @param clave Clave de acceso de la retención
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @return Array con [mensaje, pathXMLRelativo, pathXMLAbsoluto]
	 * @throws Throwable
	 */
	String[] generarXMLRetencionV2(String clave, Long ambiente) throws Throwable;
	
	/**
	 * Autoriza el comprobante de retención V2 electrónica ante el SRI.
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
	String autorizarRetencionV2(Long idFacturador, Long ambiente, Long conectaSRI, String clave, 
			Long codigoRetencion, String xml, String destinatario, String pathLogo) throws Throwable;

}
