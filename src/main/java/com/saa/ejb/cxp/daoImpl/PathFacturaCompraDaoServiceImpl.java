package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.PathFacturaCompraDaoService;
import com.saa.model.cxp.PathFacturaCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PathFacturaCompraDaoServiceImpl extends EntityDaoImpl<PathFacturaCompra> implements PathFacturaCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","factura","path","alterno"};
	}
}
