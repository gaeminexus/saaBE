package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.FormaPagoFacturaCompraDaoService;
import com.saa.model.cxp.FormaPagoFacturaCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class FormaPagoFacturaCompraDaoServiceImpl extends EntityDaoImpl<FormaPagoFacturaCompra> implements FormaPagoFacturaCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","factura","formaPago","valor","plazo","unidadTiempo"};
	}
}
