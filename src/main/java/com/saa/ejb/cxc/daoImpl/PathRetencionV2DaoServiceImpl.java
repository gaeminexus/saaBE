package com.saa.ejb.cxc.daoImpl;

import java.util.List;

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

	@Override
	public PathRetencionV2 selectUltimoFirmadoByRetencion(Long idRetencion) throws Throwable {
		List<PathRetencionV2> resultado = em.createQuery(
				"select p from PathRetencionV2 p where p.retencionV2.id = :idRetencion and p.alterno = 3 "
						+ "order by p.id desc", PathRetencionV2.class)
				.setParameter("idRetencion", idRetencion)
				.setMaxResults(1)
				.getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
	}
}
