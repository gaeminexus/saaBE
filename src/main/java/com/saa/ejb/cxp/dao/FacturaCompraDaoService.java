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
}
