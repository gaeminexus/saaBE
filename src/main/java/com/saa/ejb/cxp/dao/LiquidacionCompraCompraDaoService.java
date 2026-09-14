package com.saa.ejb.cxp.dao;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.LiquidacionCompraCompra;
import jakarta.ejb.Local;
@Local
public interface LiquidacionCompraCompraDaoService extends EntityDao<LiquidacionCompraCompra> {

	/**
	 * Liquidaciones de compra activas cuyo sustento tributario SRI (LQCCCSUS) no se pudo
	 * resolver automáticamente ni se ha corregido a mano. Es la lista que hay que repasar
	 * antes de generar el ATS. Mismo criterio que {@code FacturaCompraDaoService.selectPendientesSustento}.
	 *
	 * @param idEmpresa		: Empresa a filtrar; null = todas
	 * @return				: Liquidaciones con sustento pendiente, más recientes primero
	 * @throws Throwable	: Excepcion
	 */
	List<LiquidacionCompraCompra> selectPendientesSustento(Long idEmpresa) throws Throwable;

	/**
	 * Liquidaciones de compra activas (no anuladas) de un proveedor, para calcular
	 * cuáles ya tienen el saldo íntegramente comprometido por pagos. Equivalente de
	 * {@code FacturaCompraDaoService.selectActivasByTitular}
	 * (docs/logica-negocio/cxp/DISENO-FACTURAS-COMPROMETIDAS-EN-COMBO-PAGOS.md).
	 * @param idTitular  : Id del proveedor
	 * @return           : Liquidaciones activas y no anuladas del proveedor
	 * @throws Throwable : Excepcion
	 */
	List<LiquidacionCompraCompra> selectActivasByTitular(Long idTitular) throws Throwable;
}
