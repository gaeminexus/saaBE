package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.PathNotaDebitoDaoService;
import com.saa.model.cxc.PathNotaDebito;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PathNotaDebitoDaoServiceImpl extends EntityDaoImpl<PathNotaDebito> implements PathNotaDebitoDaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "notaDebito", "path", "alterno"};
	}
}
