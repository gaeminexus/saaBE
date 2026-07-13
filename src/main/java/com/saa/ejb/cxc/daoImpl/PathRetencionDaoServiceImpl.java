package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.PathRetencionDaoService;
import com.saa.model.cxc.PathRetencion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PathRetencionDaoServiceImpl extends EntityDaoImpl<PathRetencion> implements PathRetencionDaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "retencion", "path", "alterno"};
	}
}
