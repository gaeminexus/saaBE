package com.saa.ejb.credito.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.CargaArchivoDaoService;
import com.saa.model.crd.CargaArchivo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;



@Stateless
public class CargaArchivoDaoServiceImpl extends EntityDaoImpl<CargaArchivo> implements CargaArchivoDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	@SuppressWarnings("unchecked")
	public List<CargaArchivo> selectByAnio(Long anio) throws Throwable {
		System.out.println("Ingresa al metodo selectByAnio con anio:" + anio);
		Query query = em.createQuery(" select b " +
									 " from   CargaArchivo b " +
									 " where  b.anioAfectacion = :anio");			
		query.setParameter("anio", anio);
		return query.getResultList();
	}
}
