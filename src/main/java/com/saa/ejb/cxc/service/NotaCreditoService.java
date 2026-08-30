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

	// =========================================================================
	// Etapas transaccionales independientes del proceso de emisión
	// -------------------------------------------------------------------------
	// procesarNotaCreditoCompleta las invoca a través del contenedor
	// (SessionContext.getBusinessObject) para que cada una corra en su propia
	// transacción: un fallo tardío jamás debe reversar una NC ya autorizada.
	// =========================================================================

	/**
	 * Emite la nota de crédito ante el SRI en una transacción propia
	 * (REQUIRES_NEW): valida cuentas, genera y firma el XML, envía a recepción
	 * y —sólo si el SRI la acepta— graba el documento y persiste la autorización.
	 * @return Mapa con clave, idNotaCredito y emitida=true si el SRI la autorizó
	 */
	java.util.Map<String, Object> emitirNotaCreditoAnteSRI(NotaCredito notaCredito,
			java.util.List<com.saa.model.cxc.DetalleNotaCredito> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;

	/**
	 * Genera y vincula el asiento contable de una nota de crédito en
	 * transacción propia (REQUIRES_NEW). Idempotente.
	 * @param idNotaCredito Id de la nota de crédito ya autorizada
	 * @return Mapa con aplica, generado, yaExistia, idAsiento, numeroAlterno
	 */
	java.util.Map<String, Object> generarContabilidadNotaCredito(Long idNotaCredito) throws Throwable;

	/**
	 * Registra el abono de la nota de crédito sobre la factura afectada, en
	 * transacción propia (REQUIRES_NEW). Idempotente.
	 * @param idNotaCredito Id de la nota de crédito (debe tener asiento)
	 * @return Mapa con aplicado, yaExistia, idAplicacion
	 */
	java.util.Map<String, Object> aplicarPagoNotaCredito(Long idNotaCredito) throws Throwable;

	/**
	 * Marca la nota de crédito como autorizada por el SRI en transacción propia
	 * (REQUIRES_NEW): estado 5, autorización y XML autorizado. Idempotente.
	 * @return true si actualizó el estado, false si ya estaba autorizada
	 */
	boolean marcarNotaCreditoAutorizada(Long idNotaCredito, String numeroAutorizacion,
			String fechaAutorizacion, String comprobanteXML) throws Throwable;

	/** Reenvía (o envía por primera vez) el email de una nota de crédito autorizada.
	 *  Si el PDF no existe en disco lo regenera al vuelo. */
	java.util.Map<String, Object> reenviarEmail(Long idNotaCredito, String destinatarios) throws Throwable;

    /**
     * Anula una nota de crédito y su asiento contable vinculado.
     * <p>
     * <b>Cambio de comportamiento (ítem 14, 2026-08-28):</b> esta nota puede haber sido usada
     * como instrumento para pagar una factura de venta ({@code AplicacionPagoCxc.notaCredito}).
     * Antes se reversaba ese cruce SIEMPRE, en silencio. Ahora, si existe y
     * {@code anularEnCascada} es {@code false} (default), se rechaza con
     * {@code IncomeException} listando qué factura(s) pagó; con {@code true} se reversa
     * primero y luego se anula.
     * @param idNotaCredito		: ID de la nota de crédito
     * @param motivo			: Motivo de anulación
     * @param usuario			: Usuario que realiza la anulación
     * @param idUsuario			: Id del usuario (SCP.PJRQ), para reversar el cruce si aplica
     * @param anularEnCascada	: true = reversar el cruce contra la factura y anular igual
     * @return					: Map con resultado de la operación
     * @throws Throwable		: Si tiene un cruce activo sin reversar y no viene cascada
     */
    java.util.Map<String, Object> anularNotaCredito(Long idNotaCredito, String motivo, String usuario,
            Long idUsuario, boolean anularEnCascada) throws Throwable;

    /**
     * Facturas de venta que esta nota de crédito está pagando actualmente. Ver
     * {@link #anularNotaCredito}.
     * @param idNotaCredito	: Id de la nota de crédito
     * @return				: Lista de mapas con idAplicacion, idFactura, montoAplicado,
     *						  fechaAplicacion; vacía si no está pagando ninguna factura
     * @throws Throwable	: Excepcion
     */
    java.util.List<java.util.Map<String, Object>> movimientosRelacionadosNotaCredito(Long idNotaCredito)
            throws Throwable;

    /**
     * Consulta el estado de una nota de crédito ante el SRI (WS consultarEstadoAutorizacion).
     * Si el SRI devuelve AUTORIZADO:
     *   - Actualiza el estado de la nota de crédito a autorizada si estaba pendiente.
     *   - Establece el número de autorización y fecha de autorización.
     *   - Si la nota de crédito no tiene asiento contable y el facturador tiene generaConta=1,
     *     genera el asiento contable automáticamente.
     *   - Envía el email con el XML autorizado y PDF RIDE adjuntos.
     *
     * @param idNotaCredito ID de la nota de crédito a consultar
     * @return Mapa con: exito, estadoSRI, numeroAutorizacion, asientoGenerado, emailEnviado, mensaje
     */
    java.util.Map<String, Object> consultarYActualizarEstadoNotaCredito(Long idNotaCredito) throws Throwable;

}
