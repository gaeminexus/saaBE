package com.saa.ejb.cxp.dao;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.NotaCreditoCompra;
import jakarta.ejb.Local;
@Local
public interface NotaCreditoCompraDaoService extends EntityDao<NotaCreditoCompra> {

	/**
	 * Notas de crédito de compra activas cuyo sustento tributario SRI (NTCCCSUS) no se pudo
	 * resolver automáticamente ni se ha corregido a mano. Es la lista que hay que repasar
	 * antes de generar el ATS. Mismo criterio que {@code FacturaCompraDaoService.selectPendientesSustento}.
	 *
	 * @param idEmpresa		: Empresa a filtrar; null = todas
	 * @return				: Notas de crédito con sustento pendiente, más recientes primero
	 * @throws Throwable	: Excepcion
	 */
	List<NotaCreditoCompra> selectPendientesSustento(Long idEmpresa) throws Throwable;
}
