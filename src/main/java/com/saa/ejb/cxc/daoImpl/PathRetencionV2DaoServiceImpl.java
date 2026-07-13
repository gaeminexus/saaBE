package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.PathRetencionV2DaoService;
import com.saa.model.cxc.PathRetencionV2;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class PathRetencionV2DaoServiceImpl extends EntityDaoImpl<PathRetencionV2> implements PathRetencionV2DaoService {

	@PersistenceContext
	EntityManager em;
	
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "retencionV2", "path", "alterno"};
	}
}
