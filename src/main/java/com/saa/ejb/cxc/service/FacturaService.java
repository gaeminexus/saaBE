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

	// =========================================================================
	// Etapas transaccionales independientes del proceso de emisión
	// -------------------------------------------------------------------------
	// procesarFacturaCompleta las invoca a través del contenedor
	// (SessionContext.getBusinessObject) para que cada una corra en su propia
	// transacción: un fallo tardío jamás debe reversar una factura ya
	// autorizada por el SRI.
	// =========================================================================

	/**
	 * Emite la factura ante el SRI en una transacción propia (REQUIRES_NEW):
	 * prepara los campos, genera y firma el XML, envía a recepción y —sólo si
	 * el SRI la acepta— graba el documento y persiste la autorización.
	 * @return Mapa con clave, idFactura, idFacturador, factura, destinatario,
	 *         pdfBytes y emitida=true si el SRI la autorizó
	 */
	java.util.Map<String, Object> emitirFacturaAnteSRI(com.saa.model.cxc.Factura factura,
			java.util.List<com.saa.model.cxc.DetalleFactura> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;

	/**
	 * Genera y vincula el asiento contable de una factura en transacción propia
	 * (REQUIRES_NEW). Idempotente: si ya tiene asiento no genera otro.
	 * @param idFactura Id de la factura ya autorizada
	 * @return Mapa con aplica, generado, yaExistia, idAsiento, numeroAlterno
	 */
	java.util.Map<String, Object> generarContabilidadFactura(Long idFactura) throws Throwable;

	/**
	 * Marca la factura como autorizada por el SRI en transacción propia
	 * (REQUIRES_NEW): estado 5, autorización y XML autorizado. Idempotente.
	 * @return true si actualizó el estado, false si ya estaba autorizada
	 */
	boolean marcarFacturaAutorizada(Long idFactura, String numeroAutorizacion,
			String fechaAutorizacion, String comprobanteXML) throws Throwable;

	/**
	 * MÉTODO DE PRUEBA: Envía un XML correcto al SRI para probar la comunicación
	 */
	String probarEnvioXMLCorrecto(String xmlCorrecto) throws Exception;

	/**
	 * Reintenta la autorización de una factura que ya fue enviada al SRI pero
	 * quedó en estado pendiente. Solo llama al WS2 de autorización (NO reenvía
	 * al WS1 de recepción). Actualiza el estado de la factura si se autoriza.
	 *
	 * @param idFactura ID de la factura a reintentar
	 * @return Mapa con el resultado: exito, estado, numeroAutorizacion, mensaje
	 * @throws Throwable Si la factura no existe o no está en estado enviado/pendiente
	 */
	java.util.Map<String, Object> reintentarAutorizacion(Long idFactura) throws Throwable;

	/**
	 * Reenvía el correo electrónico de una factura ya autorizada a una lista
	 * de destinatarios separados por punto y coma (;).
	 * Adjunta el XML autorizado y el PDF RIDE si existen en disco.
	 *
	 * @param idFactura    ID de la factura a reenviar
	 * @param destinatarios Cadena con emails separados por ; (ej: "a@x.com;b@y.com")
	 * @return Mapa con el resultado: exito, emailsEnviados, mensaje
	 * @throws Throwable Si la factura no existe o no está autorizada
	 */
        java.util.Map<String, Object> reenviarEmail(Long idFactura, String destinatarios) throws Throwable;

	/**
	 * Consulta el estado de una factura ante el SRI (WS consultarEstadoAutorizacion).
	 * Si el SRI devuelve AUTORIZADO:
	 *   - Actualiza el estado de la factura a 5 (emitida/autorizada) si estaba pendiente.
	 *   - Establece el número de autorización y fecha de autorización.
	 *   - Si la factura no tiene asiento contable y el facturador tiene generaConta=1,
	 *     genera el asiento contable automáticamente.
	 *
	 * @param idFactura ID de la factura a consultar
	 * @return Mapa con: exito, estadoSRI, numeroAutorizacion, asientoGenerado, mensaje
	 */
	java.util.Map<String, Object> consultarYActualizarEstadoFactura(Long idFactura) throws Throwable;

        /**
         * Anula una factura cambiando su estado a INACTIVO (2) y anula
         * el asiento contable vinculado si existe.
         *
         * @param idFactura ID de la factura a anular
         * @param motivo    Motivo de la anulación (opcional)
         * @return Mapa con exito, mensaje
         * @throws Throwable Si la factura no existe o ya está anulada
         */
        java.util.Map<String, Object> anularFactura(Long idFactura, String motivo, String usuario) throws Throwable;
}
