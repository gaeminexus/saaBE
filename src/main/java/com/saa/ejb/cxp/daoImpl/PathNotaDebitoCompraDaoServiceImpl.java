package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.PathNotaDebitoCompraDaoService;
import com.saa.model.cxp.PathNotaDebitoCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PathNotaDebitoCompraDaoServiceImpl extends EntityDaoImpl<PathNotaDebitoCompra> implements PathNotaDebitoCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","notaDebito","path","alterno"};
	}
}
