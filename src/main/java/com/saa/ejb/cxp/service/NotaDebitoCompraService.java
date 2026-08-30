package com.saa.ejb.cxp.service;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.NotaDebitoCompra;
import jakarta.ejb.Local;
@Local
public interface NotaDebitoCompraService extends EntityService<NotaDebitoCompra> {

	/**
	 * Anula una nota de débito de compra (2026-08-28, ampliado en ítem 13) — mismo criterio que
	 * {@code NotaCreditoCompraService.anularNotaCreditoCompra}: puede haber sido usada como
	 * instrumento de pago de una factura de compra; si es así y {@code anularEnCascada} es
	 * {@code false}, se rechaza; si es {@code true}, se reversan esos cruces primero.
	 * @param idNotaDebito		: Id de la nota de débito de compra
	 * @param motivo			: Motivo de la anulación (vacío = "Anulación manual")
	 * @param usuario			: Usuario que anula (vacío = "SISTEMA")
	 * @param idUsuario			: Id del usuario (SCP.PJRQ), para reversar las aplicaciones
	 * @param anularEnCascada	: true = reversar los cruces contra facturas y anular igual
	 * @return					: Mapa con exito, mensaje, asientoAnulado y movimientosReversados
	 * @throws Throwable		: Excepcion
	 */
	java.util.Map<String, Object> anularNotaDebitoCompra(Long idNotaDebito, String motivo, String usuario,
			Long idUsuario, boolean anularEnCascada) throws Throwable;

	/**
	 * Facturas de compra que esta nota de débito está afectando actualmente (aplicaciones
	 * activas donde ella es el instrumento). Ver {@link #anularNotaDebitoCompra}.
	 * @param idNotaDebito	: Id de la nota de débito de compra
	 * @return				: Lista de mapas con idAplicacion, idFacturaCompra, montoAplicado,
	 *						  fechaAplicacion; vacía si no afecta ninguna factura
	 * @throws Throwable	: Excepcion
	 */
	java.util.List<java.util.Map<String, Object>> movimientosRelacionadosNotaDebito(Long idNotaDebito)
			throws Throwable;
}
