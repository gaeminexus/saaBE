package com.saa.ejb.cxp.service;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.NotaCreditoCompra;
import jakarta.ejb.Local;
@Local
public interface NotaCreditoCompraService extends EntityService<NotaCreditoCompra> {

	/**
	 * Anula una nota de crédito de compra (2026-08-28, ampliado en ítem 13). Una nota de
	 * crédito no RECIBE pagos, pero puede haber sido usada ELLA MISMA como instrumento para
	 * pagar una factura de compra (vía {@code AplicacionPagoCxp.notaCredito}) — si es así y
	 * {@code anularEnCascada} es {@code false}, se rechaza listando qué factura(s) pagó; si es
	 * {@code true}, se reversa cada aplicación primero (ninguna es de tipo pago directo, así
	 * que no hace falta pasar por {@code PagoProgramadoService}).
	 * @param idNotaCredito		: Id de la nota de crédito de compra
	 * @param motivo			: Motivo de la anulación (vacío = "Anulación manual")
	 * @param usuario			: Usuario que anula (vacío = "SISTEMA")
	 * @param idUsuario			: Id del usuario (SCP.PJRQ), para reversar las aplicaciones
	 * @param anularEnCascada	: true = reversar los cruces contra facturas y anular igual
	 * @return					: Mapa con exito, mensaje, asientoAnulado y movimientosReversados
	 * @throws Throwable		: Excepcion
	 */
	java.util.Map<String, Object> anularNotaCreditoCompra(Long idNotaCredito, String motivo, String usuario,
			Long idUsuario, boolean anularEnCascada) throws Throwable;

	/**
	 * Facturas de compra que esta nota de crédito está pagando actualmente (aplicaciones
	 * activas donde ella es el instrumento de pago). Ver {@link #anularNotaCreditoCompra}.
	 * @param idNotaCredito	: Id de la nota de crédito de compra
	 * @return				: Lista de mapas con idAplicacion, idFacturaCompra, montoAplicado,
	 *						  fechaAplicacion; vacía si no está pagando ninguna factura
	 * @throws Throwable	: Excepcion
	 */
	java.util.List<java.util.Map<String, Object>> movimientosRelacionadosNotaCredito(Long idNotaCredito)
			throws Throwable;
}
