package com.saa.ejb.cxc.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.PuntoEmisionDaoService;
import com.saa.model.cxc.PuntoEmision;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación DAO para la entidad PuntoEmision
 */
@Stateless
public class PuntoEmisionDaoServiceImpl extends EntityDaoImpl<PuntoEmision> implements PuntoEmisionDaoService {

	@PersistenceContext
	EntityManager em;
	
	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id", "codigo", "establecimiento", "nombre", "creado",
			"observacion", "transportista", "estado"
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PuntoEmision> selectByEstablecimiento(Long idEstablecimiento) throws Throwable {
		System.out.println("PuntoEmisionDaoServiceImpl.selectByEstablecimiento - idEstablecimiento: " + idEstablecimiento);
		Query query = em.createQuery(
			"SELECT p FROM PuntoEmision p WHERE p.establecimiento.id = :idEstablecimiento ORDER BY p.codigo"
		);
		query.setParameter("idEstablecimiento", idEstablecimiento);
		return query.getResultList();
	}
}
