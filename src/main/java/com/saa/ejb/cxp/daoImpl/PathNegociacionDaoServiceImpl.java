package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.PathNegociacionDaoService;
import com.saa.model.cxp.PathNegociacion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PathNegociacionDaoServiceImpl extends EntityDaoImpl<PathNegociacion> implements PathNegociacionDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","negociacion","path","nombreDoc","tipoDoc","principal","adendum"};
	}
}
