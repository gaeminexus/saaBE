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
	 * @return Mapa con: mensaje (String), pdfBytes (byte[]) - los bytes del PDF generado
	 * @throws Throwable
	 */
	java.util.Map<String, Object> autorizarRetencionV2(Long idFacturador, Long ambiente, Long conectaSRI, String clave,
			Long codigoRetencion, String xml, String destinatario, String pathLogo) throws Throwable;

	/**
	 * Procesa una retención electrónica V2 completa: graba, genera XML, firma y autoriza ante el SRI.
	 * Si el facturador tiene generaConta=1 y empresa contable configurada, genera el asiento contable
	 * automáticamente tras la autorización del SRI.
	 * <p>
	 * TODO — El asiento contable se disparará con la plantilla
	 *        {@code TipoAsientos.RETENCIONES_EMITIDAS_V2} una vez que se defina en BD.
	 *
	 * @param retencion   Objeto RetencionV2 a procesar (detalles ya persistidos antes de llamar)
	 * @param ambiente    Ambiente SRI (1=PRUEBA, 2=PRODUCCION). null → usa 1
	 * @param conectaSRI  Si conecta al SRI (1=SI, 0=NO). null → usa 1
	 * @param destinatario Email del proveedor. null → se obtiene del proveedor
	 * @param pathLogo    Path del logo. null → usa default
	 * @return Mapa con: exito, idRetencion, claveAcceso, autorizacion, estado,
	 *         asiento (número alterno si se generó), advertenciaAsiento (si falló el asiento)
	 * @throws Throwable si ocurre error grave en grabado, XML, firma o autorización
	 */
	java.util.Map<String, Object> procesarRetencionV2Completa(RetencionV2 retencion,
			java.util.List<com.saa.model.cxc.DetalleRetencionV2> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;

	// =========================================================================
	// Etapas transaccionales independientes del proceso de emisión
	// -------------------------------------------------------------------------
	// Se exponen en la interfaz porque procesarRetencionV2Completa las invoca a
	// través del contenedor (SessionContext.getBusinessObject) para que cada una
	// corra en su propia transacción. Un fallo tardío jamás debe reversar una
	// retención ya autorizada por el SRI.
	// =========================================================================

	/**
	 * Graba la retención V2 junto con sus detalles en una transacción propia
	 * (REQUIRES_NEW) que se confirma de inmediato.
	 * @param retencion : Retención a grabar
	 * @param detalles  : Detalles de la retención (puede ser null o vacío)
	 * @return : La retención grabada, con id, número y clave de acceso asignados
	 */
	RetencionV2 grabarRetencionV2ConDetalles(RetencionV2 retencion,
			java.util.List<com.saa.model.cxc.DetalleRetencionV2> detalles) throws Throwable;

	/**
	 * Genera y vincula el asiento contable de una retención V2 en transacción
	 * propia (REQUIRES_NEW). Es idempotente: si la retención ya tiene asiento
	 * no genera otro.
	 * @param idRetencion : Id de la retención V2
	 * @return : Mapa con aplica (el facturador genera contabilidad), generado,
	 *           yaExistia, idAsiento, numeroAlterno
	 */
	java.util.Map<String, Object> generarContabilidadRetencionV2(Long idRetencion) throws Throwable;

	/**
	 * Registra el cruce (abono) de la retención V2 sobre la factura de compra
	 * afectada, en transacción propia (REQUIRES_NEW). Es idempotente: si el
	 * cruce ya existe no lo duplica.
	 * @param idRetencion : Id de la retención V2 (debe tener asiento contable)
	 * @return : Mapa con aplicado, yaExistia, idAplicacion, idFactura
	 */
	java.util.Map<String, Object> aplicarPagoRetencionV2(Long idRetencion) throws Throwable;

	/**
	 * Elimina la retención V2 con sus detalles y paths, en transacción propia
	 * (REQUIRES_NEW). Sólo debe usarse cuando el comprobante NUNCA llegó a ser
	 * aceptado por el SRI (error de XML/firma o recepción DEVUELTA).
	 * @param idRetencion : Id de la retención V2 a eliminar
	 */
	void eliminarRetencionV2NoEmitida(Long idRetencion) throws Throwable;

	/**
	 * Marca la retención V2 como autorizada por el SRI en transacción propia
	 * (REQUIRES_NEW): estado 5, número y fecha de autorización, y XML
	 * autorizado en disco. Es idempotente.
	 * @param idRetencion        : Id de la retención V2
	 * @param numeroAutorizacion : Número de autorización devuelto por el SRI
	 * @param fechaAutorizacion  : Fecha de autorización devuelta por el SRI
	 * @param comprobanteXML     : XML autorizado (puede ser null)
	 * @return : true si actualizó el estado, false si ya estaba autorizada
	 */
	boolean marcarRetencionV2Autorizada(Long idRetencion, String numeroAutorizacion,
			String fechaAutorizacion, String comprobanteXML) throws Throwable;

	/** Reenvía (o envía por primera vez) el email de una retención V2 autorizada.
	 *  Si el PDF no existe en disco lo regenera al vuelo. */
	java.util.Map<String, Object> reenviarEmail(Long idRetencion, String destinatarios) throws Throwable;

	/**
	 * Anula una retención V2 y su asiento contable vinculado.
	 * <p>
	 * <b>Cambio de comportamiento (ítem 15, 2026-08-28):</b> mismo criterio que
	 * {@code FacturaService.anularFactura} y los demás documentos del ítem 14. Esta
	 * retención V2, aunque vive en el paquete {@code cxc} (es un documento electrónico
	 * emitido por la empresa), es en realidad un instrumento de pago del lado
	 * <b>compra</b>: reduce facturas de compra vía {@code AplicacionPagoCxp}
	 * (tipoDocPago RETENCION), no {@code AplicacionPagoCxc}. Si tiene aplicaciones
	 * activas y {@code anularEnCascada} es {@code false} (default), se rechaza con
	 * {@code IncomeException}; con {@code true} se reversan primero.
	 * @param idRetencion		: ID de la retención V2
	 * @param motivo			: Motivo de la anulación
	 * @param usuario			: Usuario que realiza la anulación
	 * @param idUsuario			: Id del usuario (SCP.PJRQ), para reversar aplicaciones si aplica
	 * @param anularEnCascada	: true = reversar las aplicaciones sobre facturas de compra y anular igual
	 * @return					: Mapa con: exito, mensaje, idRetencion, motivoAnulacion, fechaAnulacion, usuarioAnulacion
	 * @throws Throwable		: Si tiene aplicaciones activas sin reversar y no viene cascada
	 */
	java.util.Map<String, Object> anularRetencionV2(Long idRetencion, String motivo, String usuario,
			Long idUsuario, boolean anularEnCascada) throws Throwable;

	/**
	 * Aplicaciones de pago (AplicacionPagoCxp) activas donde esta retención V2 es el
	 * instrumento usado para reducir una factura de compra. Ver {@link #anularRetencionV2}.
	 * @param idRetencion	: Id de la retención V2
	 * @return				: Lista de mapas con idAplicacion, idFacturaCompra, montoAplicado,
	 *						  fechaAplicacion; vacía si no afecta ninguna factura de compra
	 * @throws Throwable	: Excepcion
	 */
	java.util.List<java.util.Map<String, Object>> movimientosRelacionadosRetencionV2(Long idRetencion)
			throws Throwable;

	/**
	 * Consulta el estado de una retención V2 ante el SRI (WS consultarEstadoAutorizacion).
	 * Si el SRI devuelve AUTORIZADO:
	 *   - Actualiza el estado de la retención a autorizada si estaba pendiente.
	 *   - Establece el número de autorización y fecha de autorización.
	 *   - Si la retención no tiene asiento contable y el facturador tiene generaConta=1,
	 *     genera el asiento contable automáticamente.
	 *   - Envía el email con el XML autorizado y PDF RIDE adjuntos.
	 *
	 * @param idRetencion ID de la retención V2 a consultar
	 * @return Mapa con: exito, estadoSRI, numeroAutorizacion, asientoGenerado, emailEnviado, mensaje
	 */
	java.util.Map<String, Object> consultarYActualizarEstadoRetencionV2(Long idRetencion) throws Throwable;

}
