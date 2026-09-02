package com.saa.ejb.cxp.service;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.LiquidacionCompraCompra;
import jakarta.ejb.Local;
@Local
public interface LiquidacionCompraCompraService extends EntityService<LiquidacionCompraCompra> {

	/**
	 * Anula una liquidación de compra — mismo criterio que
	 * {@code FacturaCompraService.anularFacturaCompra}, cascada incluida.
	 * <p>
	 * Antes de admitir el cruce de anticipos contra liquidaciones
	 * (docs/logica-negocio/cxp/DISENO-CRUCE-ANTICIPO-CONTRA-LIQUIDACION.md §5 punto 5)
	 * una liquidación nunca podía tener aplicaciones, así que esta anulación no
	 * cascadeaba nada. Con el cruce activo eso deja de ser cierto: si hay
	 * aplicaciones activas y no viene {@code anularEnCascada}, se rechaza con
	 * {@code IncomeException} (el REST la convierte en 409) en vez de dejarlas
	 * huérfanas en silencio.
	 * @param idLiquidacion	 : Id de la liquidación de compra
	 * @param motivo		 : Motivo de la anulación (vacío = "Anulación manual")
	 * @param usuario		 : Usuario que anula (vacío = "SISTEMA")
	 * @param idUsuario		 : Id del usuario que anula (sólo hace falta si anularEnCascada=true)
	 * @param anularEnCascada: true para reversar las aplicaciones activas antes de anular
	 * @return				 : Mapa con exito, mensaje, movimientosReversados y asientoAnulado si aplica
	 * @throws Throwable	 : IncomeException si hay aplicaciones activas sin anularEnCascada
	 */
	java.util.Map<String, Object> anularLiquidacionCompra(Long idLiquidacion, String motivo, String usuario,
			Long idUsuario, boolean anularEnCascada) throws Throwable;

	/**
	 * Movimientos relacionados (aplicaciones de pago activas) de una liquidación
	 * de compra — para que el frontend muestre la lista antes de preguntar
	 * "¿anular con todos los movimientos?", igual que
	 * {@code FacturaCompraService.movimientosRelacionadosFactura}.
	 * @param idLiquidacion : Id de la liquidación de compra
	 * @return              : Lista de mapas con idAplicacion, tipoDocPago, tipoDocPagoTexto,
	 *                        montoAplicado, fechaAplicacion
	 * @throws Throwable    : Excepcion
	 */
	java.util.List<java.util.Map<String, Object>> movimientosRelacionadosLiquidacion(Long idLiquidacion)
			throws Throwable;
}
