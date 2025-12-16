package com.saa.ejb.credito.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.DireccionDaoService;
import com.saa.model.credito.Direccion;

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
								 	 " where  d.entidad.codigo = :idIdentidad");
		query.setParameter("idIdentidad", idIdentidad);
		return query.getResultList();
	}

}
