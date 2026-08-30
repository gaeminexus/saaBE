package com.saa.ejb.cxp.service;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.FacturaCompra;
import jakarta.ejb.Local;
@Local
public interface FacturaCompraService extends EntityService<FacturaCompra> {

	/**
	 * Anula una factura de compra (2026-08-28, ampliado en ítem 13 con cascada): valida que no
	 * esté ya anulada, marca {@code estadoEmision=3} (ANULADA, mismo código que
	 * {@code FacturaServiceImpl.anularFactura} del lado venta) y {@code estado=INACTIVO}, graba
	 * motivo/fecha/usuario, y anula el asiento contable vinculado si existe.
	 * <p>
	 * <b>Movimientos relacionados (pagos, notas de crédito/débito, retenciones, anticipos
	 * cruzados contra esta factura vía {@code AplicacionPagoCxp}):</b> si existen y
	 * {@code anularEnCascada} es {@code false}, se rechaza con {@code IncomeException} listando
	 * qué se encontró — decisión del usuario (2026-08-28): "no debe permitir anular una factura
	 * si ya se cruzó un anticipo o se hizo un pago o se cruzó una retención, salvo que se anulen
	 * todos los movimientos relacionados con esa factura". Si {@code anularEnCascada} es
	 * {@code true}, se reversa cada movimiento primero (por el camino correcto según su tipo —
	 * ver {@link #movimientosRelacionadosFactura(Long)}) y recién entonces se anula la factura,
	 * todo en la misma transacción.
	 *
	 * @param idFactura			: Id de la factura de compra
	 * @param motivo			: Motivo de la anulación (vacío = "Anulación manual")
	 * @param usuario			: Usuario que anula (vacío = "SISTEMA")
	 * @param idUsuario			: Id del usuario (SCP.PJRQ), lo pide {@code revertirPagoConfirmado}
	 *							  para los movimientos de tipo pago directo; puede ser null si no
	 *							  hay ninguno de ese tipo entre los movimientos a reversar
	 * @param anularEnCascada	: true = reversar todos los movimientos relacionados y anular
	 *							  igual; false = rechazar si hay alguno
	 * @return					: Mapa con exito, mensaje, asientoAnulado y movimientosReversados
	 *							  (cuántos, si hubo cascada)
	 * @throws Throwable		: Excepcion
	 */
	java.util.Map<String, Object> anularFacturaCompra(Long idFactura, String motivo, String usuario,
			Long idUsuario, boolean anularEnCascada) throws Throwable;

	/**
	 * Movimientos relacionados a una factura de compra: pagos directos, notas de crédito/débito
	 * y retenciones/anticipos cruzados contra ella, todos vía {@code AplicacionPagoCxp} activa
	 * (estado=1). Es la lista que el frontend muestra antes de preguntar "¿anular con todos los
	 * movimientos?" (ítem 13).
	 *
	 * @param idFactura	: Id de la factura de compra
	 * @return			: Lista de mapas con idAplicacion, tipoDocPago, tipoDocPagoTexto,
	 *					  montoAplicado, fechaAplicacion; vacía si no tiene movimientos
	 * @throws Throwable: Excepcion
	 */
	java.util.List<java.util.Map<String, Object>> movimientosRelacionadosFactura(Long idFactura)
			throws Throwable;
}
