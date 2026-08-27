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
	 * Procesa una liquidación de compra completa: valida cuentas contables,
	 * graba, genera XML, firma y autoriza ante el SRI; al autorizarse crea el
	 * documento CXP (cuenta por pagar) y envía el email con el RIDE.
	 *
	 * @param liquidacion Objeto LiquidacionCompra con todos los datos
	 * @param detalles Detalles de la liquidación (cada uno con producto asignado)
	 * @param formasPago Formas de pago de la liquidación (tabla SRI 24); si viene vacía se usa "01" (Sin utilización sistema financiero)
	 * @param ambiente Ambiente (se usa 1=PRUEBA por defecto)
	 * @param conectaSRI Si conecta al SRI (se usa 1=SI por defecto)
	 * @param destinatario Email del destinatario (se obtiene del proveedor)
	 * @param pathLogo Path del logo (se usa logo estándar)
	 * @return Mapa con el resultado del proceso completo
	 * @throws Throwable
	 */
	java.util.Map<String, Object> procesarLiquidacionCompleta(LiquidacionCompra liquidacion,
			java.util.List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles,
			java.util.List<com.saa.model.cxc.FormaPagoLiquidacion> formasPago,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;

	// =========================================================================
	// Etapas transaccionales independientes del proceso de emisión
	// -------------------------------------------------------------------------
	// procesarLiquidacionCompleta las invoca a través del contenedor
	// (SessionContext.getBusinessObject) para que cada una corra en su propia
	// transacción: un fallo tardío jamás debe reversar una liquidación ya
	// autorizada por el SRI.
	// =========================================================================

	/**
	 * Emite la liquidación de compra ante el SRI en una transacción propia
	 * (REQUIRES_NEW): genera y firma el XML, envía a recepción y —sólo si el
	 * SRI la acepta— graba el documento, sus detalles y formas de pago, y
	 * persiste la autorización.
	 * @return Mapa con clave, idLiquidacion, idFacturador, destinatario,
	 *         pdfBytes (RIDE) y emitida=true si el SRI la autorizó
	 */
	java.util.Map<String, Object> emitirLiquidacionAnteSRI(LiquidacionCompra liquidacion,
			java.util.List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles,
			java.util.List<com.saa.model.cxc.FormaPagoLiquidacion> formasPago,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;

	/**
	 * Crea el documento CXP (PGS.LQCC + detalles + path) a partir de una
	 * liquidación ya autorizada por el SRI, y lo contabiliza como liquidación
	 * de compra recibida (LIQUIDACIONES_COMPRA_RECIBIDAS). Transacción propia
	 * (REQUIRES_NEW). Idempotente: si {@code liquidacion.documentoCxp != null}
	 * no repite nada.
	 * @param idLiquidacion Id de la liquidación (CXC) ya autorizada
	 * @return Mapa con aplica, generado, yaExistia, idDocumentoCxp, idAsiento, numeroAlterno
	 */
	java.util.Map<String, Object> crearDocumentoCxp(Long idLiquidacion) throws Throwable;

	/**
	 * Marca la liquidación como autorizada por el SRI en transacción propia
	 * (REQUIRES_NEW). Idempotente.
	 * @return true si actualizó el estado, false si ya estaba autorizada
	 */
	boolean marcarLiquidacionAutorizada(Long idLiquidacion, String numeroAutorizacion,
			String fechaAutorizacion, String comprobanteXML) throws Throwable;

	/**
	 * Punto de recuperación: reintenta sólo la consulta de autorización (WS2)
	 * de una liquidación que quedó RECIBIDA/ENVIADA pero sin autorizar.
	 */
	java.util.Map<String, Object> reintentarAutorizacionLiquidacion(Long idLiquidacion) throws Throwable;

	/**
	 * Reenvía el email de una liquidación ya autorizada a uno o más
	 * destinatarios (separados por ';').
	 */
	java.util.Map<String, Object> reenviarEmailLiquidacion(Long idLiquidacion, String destinatarios) throws Throwable;

	/**
	 * Anula una liquidación de compra emitida: sólo si su documento CXP
	 * (PGS.LQCC) no tiene aplicaciones de pago registradas. Anula el asiento
	 * del documento CXP, y pasa LQCC y LQCS a estado {@code Estado.INACTIVO} (0)
	 * + LQCS.estadoEmision.
	 */
	java.util.Map<String, Object> anularLiquidacion(Long idLiquidacion, String motivo, String usuario) throws Throwable;

	/**
	 * Consulta el estado en el SRI y completa lo que haya quedado pendiente
	 * (estado, documento CXP + asiento, email). Sin transacción propia.
	 */
	java.util.Map<String, Object> consultarYActualizarEstadoLiquidacion(Long idLiquidacion) throws Throwable;

}
