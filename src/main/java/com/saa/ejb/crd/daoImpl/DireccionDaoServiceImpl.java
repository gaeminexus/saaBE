package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DireccionDaoService;
import com.saa.model.crd.Direccion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;



@Stateless
public class DireccionDaoServiceImpl extends EntityDaoImpl<Direccion> implements DireccionDaoService{
	
	@PersistenceContext
	EntityManager em;

	@SuppressWarnings("unchecked")
	@Override
	public List<Direccion> selectByParent(Long idIdentidad) {
		System.out.println("DireccionDaoServiceImpl - selectByParent - idIdentidad: " + idIdentidad);
		Query query = em.createQuery(" select d " +
								 	 " from   Direccion d " +
								 	 " where  d.entidad.codigo = :idIdentidad" +
								 	 " order by coalesce(d.porDefecto, 0) desc, d.codigo asc");
		query.setParameter("idIdentidad", idIdentidad);
		return query.getResultList();
	}

	private static final String DOMICILIOS =
		" select d from Direccion d " +
		" where  d.entidad.codigo = :idEntidad " +
		"   and  coalesce(d.estado, 1) = 1 " +
		"   and  coalesce(d.trabajo, 0) <> 1 " +
		"   and  not exists (select 1 from DireccionTrabajo t where t.direccion.codigo = d.codigo) ";

	@SuppressWarnings("unchecked")
	@Override
	public Direccion selectDomicilioVigente(Long idEntidad) {
		System.out.println("DireccionDaoServiceImpl - selectDomicilioVigente - idEntidad: " + idEntidad);
		if (idEntidad == null) {
			return null;
		}
		Query query = em.createQuery(DOMICILIOS +
			" order by case when coalesce(d.porDefecto, 0) = 1 then 0 else 1 end, d.codigo asc");
		query.setParameter("idEntidad", idEntidad);
		query.setMaxResults(1);
		List<Direccion> lista = query.getResultList();
		return lista.isEmpty() ? null : lista.get(0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Direccion> selectDomiciliosPorDefecto(Long idEntidad) {
		Query query = em.createQuery(DOMICILIOS + " and coalesce(d.porDefecto, 0) = 1 order by d.codigo asc");
		query.setParameter("idEntidad", idEntidad);
		return query.getResultList();
	}

}
