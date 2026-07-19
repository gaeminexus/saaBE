package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.PathNotaCreditoCompraDaoService;
import com.saa.model.cxp.PathNotaCreditoCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PathNotaCreditoCompraDaoServiceImpl extends EntityDaoImpl<PathNotaCreditoCompra> implements PathNotaCreditoCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","notaCredito","path","alterno"};
	}
}
