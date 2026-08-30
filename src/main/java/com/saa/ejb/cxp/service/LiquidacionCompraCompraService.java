package com.saa.ejb.cxp.service;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.LiquidacionCompraCompra;
import jakarta.ejb.Local;
@Local
public interface LiquidacionCompraCompraService extends EntityService<LiquidacionCompraCompra> {

	/**
	 * Anula una liquidación de compra (CXP, 2026-08-28) — mismo criterio que
	 * {@code FacturaCompraService.anularFacturaCompra}. No reversa aplicaciones de pago.
	 * @param idLiquidacion	: Id de la liquidación de compra
	 * @param motivo		: Motivo de la anulación (vacío = "Anulación manual")
	 * @param usuario		: Usuario que anula (vacío = "SISTEMA")
	 * @return				: Mapa con exito, mensaje, y asientoAnulado si aplica
	 * @throws Throwable	: Excepcion
	 */
	java.util.Map<String, Object> anularLiquidacionCompra(Long idLiquidacion, String motivo, String usuario)
			throws Throwable;
}
