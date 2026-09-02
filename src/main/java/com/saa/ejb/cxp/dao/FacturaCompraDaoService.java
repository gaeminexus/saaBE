package com.saa.ejb.cxp.dao;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.FacturaCompra;
import jakarta.ejb.Local;
@Local
public interface FacturaCompraDaoService extends EntityDao<FacturaCompra> {

	/**
	 * Facturas de compra activas cuyo sustento tributario SRI (FCTCCSUS) no se pudo resolver
	 * automáticamente ni se ha corregido a mano. Es la lista que hay que repasar antes de
	 * generar el ATS.
	 *
	 * @param idEmpresa		: Empresa a filtrar; null = todas
	 * @return				: Facturas con sustento pendiente, más recientes primero
	 * @throws Throwable	: Excepcion
	 */
	List<FacturaCompra> selectPendientesSustento(Long idEmpresa) throws Throwable;

	/**
	 * Facturas de compra activas (no anuladas) de un proveedor, para calcular
	 * cuáles ya tienen el saldo íntegramente comprometido por pagos
	 * (docs/logica-negocio/cxp/DISENO-FACTURAS-COMPROMETIDAS-EN-COMBO-PAGOS.md).
	 * @param idTitular  : Id del proveedor
	 * @return           : Facturas activas y no anuladas del proveedor
	 * @throws Throwable : Excepcion
	 */
	List<FacturaCompra> selectActivasByTitular(Long idTitular) throws Throwable;
}
