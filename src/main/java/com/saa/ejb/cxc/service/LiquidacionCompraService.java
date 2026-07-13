package com.saa.ejb.cxc.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cxc.LiquidacionCompra;
import jakarta.ejb.Local;

@Local
public interface LiquidacionCompraService extends EntityService<LiquidacionCompra> {

	/**
	 * Genera el XML de liquidación de compra electrónica según estándares del SRI.
	 * @param clave Clave de acceso de la liquidación
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @return Array con [mensaje, pathXMLRelativo, pathXMLAbsoluto]
	 * @throws Throwable
	 */
	String[] generarXMLLiquidacion(String clave, Long ambiente) throws Throwable;
	
	/**
	 * Autoriza la liquidación de compra electrónica ante el SRI.
	 * @param idFacturador ID del facturador
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @param conectaSRI Si conecta al SRI (1=SI, 0=NO)
	 * @param clave Clave de acceso
	 * @param codigoLiquidacion ID de la liquidación
	 * @param xml Contenido XML firmado
	 * @param destinatario Email del destinatario
	 * @param pathLogo Path del logo
	 * @return Mensaje con el resultado de la autorización
	 * @throws Throwable
	 */
	String autorizarLiquidacion(Long idFacturador, Long ambiente, Long conectaSRI, String clave, 
			Long codigoLiquidacion, String xml, String destinatario, String pathLogo) throws Throwable;
	
	/**
	 * Procesa una liquidación de compra completa: graba, genera XML, firma y autoriza ante el SRI.
	 * 
	 * @param liquidacion Objeto LiquidacionCompra con todos los datos
	 * @param ambiente Ambiente (se usa 1=PRUEBA por defecto)
	 * @param conectaSRI Si conecta al SRI (se usa 1=SI por defecto)
	 * @param destinatario Email del destinatario (se obtiene del proveedor)
	 * @param pathLogo Path del logo (se usa logo estándar)
	 * @return Mapa con el resultado del proceso completo
	 * @throws Throwable
	 */
	java.util.Map<String, Object> procesarLiquidacionCompleta(LiquidacionCompra liquidacion,
			java.util.List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;

}
