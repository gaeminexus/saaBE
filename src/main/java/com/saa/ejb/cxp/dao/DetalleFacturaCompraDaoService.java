package com.saa.ejb.cxp.dao;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.DetalleFacturaCompra;
import jakarta.ejb.Local;
@Local
public interface DetalleFacturaCompraDaoService extends EntityDao<DetalleFacturaCompra> {

	/**
	 * Recupera los detalles de una factura de compra.
	 *
	 * @param idFactura		: Id de la factura de compra (FCTC.ID)
	 * @return				: Detalles de esa factura; vacio si no tiene
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleFacturaCompra> selectByFactura(Long idFactura) throws Throwable;
}
