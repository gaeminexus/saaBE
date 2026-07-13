package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.PathNotaCreditoDaoService;
import com.saa.model.cxc.PathNotaCredito;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PathNotaCreditoDaoServiceImpl extends EntityDaoImpl<PathNotaCredito> implements PathNotaCreditoDaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "notaCredito", "path", "alterno"};
	}
}
