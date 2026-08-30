package com.saa.ejb.cxc.service;
import com.saa.basico.util.EntityService;
import com.saa.model.cxc.NotaDebito;
import jakarta.ejb.Local;
@Local
public interface NotaDebitoService extends EntityService<NotaDebito> {

	/**
	 * Genera el XML de nota de débito electrónica según estándares del SRI.
	 * @param clave Clave de acceso de la nota de débito
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @return Array con [mensaje, pathXMLRelativo, pathXMLAbsoluto]
	 * @throws Throwable
	 */
	String[] generarXMLNotaDebito(String clave, Long ambiente) throws Throwable;
	
	/**
	 * Autoriza la nota de débito electrónica ante el SRI.
	 * @param idFacturador ID del facturador
	 * @param ambiente Ambiente (1=PRUEBA, 2=PRODUCCION)
	 * @param conectaSRI Si conecta al SRI (1=SI, 0=NO)
	 * @param clave Clave de acceso
	 * @param codigoNotaDebito ID de la nota de débito
	 * @param xml Contenido XML firmado
	 * @param destinatario Email del destinatario
	 * @param pathLogo Path del logo
	 * @return Mensaje con el resultado de la autorización
	 * @throws Throwable
	 */
	String autorizarNotaDebito(Long idFacturador, Long ambiente, Long conectaSRI, String clave, 
			Long codigoNotaDebito, String xml, String destinatario, String pathLogo) throws Throwable;

	/**
	 * Procesa una nota de débito completa: graba, genera XML, firma y autoriza ante el SRI.
	 * Configuración automática:
	 * - ambiente: 1 (PRUEBA) si es null
	 * - conectaSRI: 1 (SI) si es null
	 * - destinatario: se obtiene del mail del comprador si es null
	 * - pathLogo: resources/logos/logo_aso.png si es null
	 * 
	 * @param notaDebito Objeto NotaDebito a procesar
	 * @param ambiente Ambiente (puede ser null para usar default)
	 * @param conectaSRI Si conecta al SRI (puede ser null para usar default)
	 * @param destinatario Email destinatario (puede ser null para usar del comprador)
	 * @param pathLogo Path del logo (puede ser null para usar default)
	 * @return Map con el resultado del proceso completo
	 * @throws Throwable
	 */
	java.util.Map<String, Object> procesarNotaDebitoCompleta(NotaDebito notaDebito,
			java.util.List<com.saa.model.cxc.DetalleNotaDebito> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;

	// =========================================================================
	// Etapas transaccionales independientes del proceso de emisión
	// -------------------------------------------------------------------------
	// procesarNotaDebitoCompleta las invoca a través del contenedor
	// (SessionContext.getBusinessObject) para que cada una corra en su propia
	// transacción: un fallo tardío jamás debe reversar una ND ya autorizada.
	// =========================================================================

	/**
	 * Emite la nota de débito ante el SRI en una transacción propia
	 * (REQUIRES_NEW): valida cuentas, genera y firma el XML, envía a recepción
	 * y —sólo si el SRI la acepta— graba el documento y persiste la autorización.
	 * @return Mapa con clave, idNotaDebito y emitida=true si el SRI la autorizó
	 */
	java.util.Map<String, Object> emitirNotaDebitoAnteSRI(NotaDebito notaDebito,
			java.util.List<com.saa.model.cxc.DetalleNotaDebito> detalles,
			Long ambiente, Long conectaSRI, String destinatario, String pathLogo) throws Throwable;

	/**
	 * Genera y vincula el asiento contable de una nota de débito en transacción
	 * propia (REQUIRES_NEW). Idempotente.
	 * @param idNotaDebito Id de la nota de débito ya autorizada
	 * @return Mapa con aplica, generado, yaExistia, idAsiento, numeroAlterno
	 */
	java.util.Map<String, Object> generarContabilidadNotaDebito(Long idNotaDebito) throws Throwable;

	/**
	 * Registra el movimiento de la nota de débito sobre la factura afectada, en
	 * transacción propia (REQUIRES_NEW). Idempotente.
	 * @param idNotaDebito Id de la nota de débito (debe tener asiento)
	 * @return Mapa con aplicado, yaExistia, idAplicacion
	 */
	java.util.Map<String, Object> aplicarPagoNotaDebito(Long idNotaDebito) throws Throwable;

	/**
	 * Marca la nota de débito como autorizada por el SRI en transacción propia
	 * (REQUIRES_NEW): estado 5, autorización y XML autorizado. Idempotente.
	 * @return true si actualizó el estado, false si ya estaba autorizada
	 */
	boolean marcarNotaDebitoAutorizada(Long idNotaDebito, String numeroAutorizacion,
			String fechaAutorizacion, String comprobanteXML) throws Throwable;

	/** Reenvía (o envía por primera vez) el email de una nota de débito autorizada.
	 *  Si el PDF no existe en disco lo regenera al vuelo. */
	java.util.Map<String, Object> reenviarEmail(Long idNotaDebito, String destinatarios) throws Throwable;

    /**
     * Anula una nota de débito y su asiento contable vinculado.
     * <p>
     * <b>Cambio de comportamiento (ítem 14, 2026-08-28):</b> mismo criterio que
     * {@code NotaCreditoService.anularNotaCredito} — si esta nota está afectando una factura
     * de venta ({@code AplicacionPagoCxc.notaDebito}) y {@code anularEnCascada} es
     * {@code false} (default), se rechaza; con {@code true} se reversa el cruce primero.
     * @param idNotaDebito		: ID de la nota de débito
     * @param motivo			: Motivo de anulación
     * @param usuario			: Usuario que realiza la anulación
     * @param idUsuario			: Id del usuario (SCP.PJRQ), para reversar el cruce si aplica
     * @param anularEnCascada	: true = reversar el cruce contra la factura y anular igual
     * @return					: Map con resultado de la operación
     * @throws Throwable		: Si tiene un cruce activo sin reversar y no viene cascada
     */
    java.util.Map<String, Object> anularNotaDebito(Long idNotaDebito, String motivo, String usuario,
            Long idUsuario, boolean anularEnCascada) throws Throwable;

    /**
     * Facturas de venta que esta nota de débito está afectando actualmente. Ver
     * {@link #anularNotaDebito}.
     * @param idNotaDebito	: Id de la nota de débito
     * @return				: Lista de mapas con idAplicacion, idFactura, montoAplicado,
     *						  fechaAplicacion; vacía si no afecta ninguna factura
     * @throws Throwable	: Excepcion
     */
    java.util.List<java.util.Map<String, Object>> movimientosRelacionadosNotaDebito(Long idNotaDebito)
            throws Throwable;

    /**
     * Consulta el estado de una nota de débito ante el SRI (WS consultarEstadoAutorizacion).
     * Si el SRI devuelve AUTORIZADO:
     *   - Actualiza el estado de la nota de débito a autorizada si estaba pendiente.
     *   - Establece el número de autorización y fecha de autorización.
     *   - Si la nota de débito no tiene asiento contable y el facturador tiene generaConta=1,
     *     genera el asiento contable automáticamente.
     *   - Envía el email con el XML autorizado y PDF RIDE adjuntos.
     *
     * @param idNotaDebito ID de la nota de débito a consultar
     * @return Mapa con: exito, estadoSRI, numeroAutorizacion, asientoGenerado, emailEnviado, mensaje
     */
    java.util.Map<String, Object> consultarYActualizarEstadoNotaDebito(Long idNotaDebito) throws Throwable;

}
