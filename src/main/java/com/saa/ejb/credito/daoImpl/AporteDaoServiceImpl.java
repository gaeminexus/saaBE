package com.saa.ejb.credito.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.AporteDaoService;
import com.saa.model.crd.Aporte;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;


@Stateless
public class AporteDaoServiceImpl extends EntityDaoImpl<Aporte> implements AporteDaoService {
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/**
	 * Filtra todos los aportes por id de entidad
	 * 
	 * @param :idEntidad
	 * @return Lista de Aporte
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Aporte> selectByEntidad(Long idEntidad) throws Throwable {
		System.out.println("metodo selectByEntidad de AporteDaoServiceImpl");
		Query query = em.createQuery(" select   b " +
				 					 " from     Aporte b " +
				 					 " where    b.entidad.codigo = :idEntidad ");
		query.setParameter("idEntidad", idEntidad);
		return  query.getResultList();
	}

	/**
	 * Cuenta la cantidad de aportes por entidad
	 */
	@Override
	public Long selectCountByEntidad(Long idEntidad) throws Throwable {
	    System.out.println("metodo selectCountByEntidad de AporteDaoServiceImpl");
	    Query query = em.createQuery( " select   count(b) " + 
	    							  " from     Aporte b " +
	    							  " where    b.entidad.codigo = :idEntidad ");
	    query.setParameter("idEntidad", idEntidad);
	    return (Long) query.getSingleResult();
	}

	

}
