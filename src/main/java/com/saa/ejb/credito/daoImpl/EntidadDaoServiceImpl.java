package com.saa.ejb.credito.daoImpl;

import java.math.BigDecimal;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.EntidadDaoService;
import com.saa.model.credito.Entidad;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class EntidadDaoServiceImpl extends EntityDaoImpl<Entidad> implements EntidadDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Entidad> selectByCodigoPetro(Long codigoPetro) throws Throwable {
		System.out.println("Ingresa al metodo selectByCodigoPetro con codigoPetro: " + codigoPetro);
		Query query = em.createQuery(" select b " +
									 " from   Entidad b" +
									 " where  b.rolPetroComercial = :codigoPetro");
		query.setParameter("codigoPetro", codigoPetro);
		return  query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<BigDecimal> selectCoincidenciasByNombre(String nombre) throws Throwable {
		System.out.println("Ingresa al metodo selectCoincidenciasByNombre de asiento con empresa: " + nombre);
		Query query = em.createNativeQuery(" select   e.ENTDCDGO " +
									 	   " from     CRD.ENTD e " +
									 	   " where    UTL_MATCH.JARO_WINKLER_SIMILARITY(e.ENTDNMCM, :nombre) > 80 ");
		query.setParameter("nombre", nombre);		
		return query.getResultList();
	}

}
