package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.CargaArchivoDaoService;
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
	
	@SuppressWarnings("unchecked")
	public List<CargaArchivo> selectByEstado(Long estado) throws Throwable {
		System.out.println("Ingresa al metodo selectByEstado con estado: " + estado);
		Query query = em.createQuery(" select b " +
									 " from   CargaArchivo b " +
									 " where  b.estado = :estado " +
									 " order by b.anioAfectacion desc, b.mesAfectacion desc");
		query.setParameter("estado", estado);
		return query.getResultList();
	}
	
	/**
	 * OPTIMIZADO: Busca la última carga procesada (MAX año/mes) directamente en BD
	 */
	@Override
	public CargaArchivo selectUltimaCargaProcesada(Long estado) throws Throwable {
		Query query = em.createQuery(
			" select b " +
			" from   CargaArchivo b " +
			" where  b.estado = :estado " +
			" order by b.anioAfectacion desc, b.mesAfectacion desc");
		query.setParameter("estado", estado);
		query.setMaxResults(1);
		
		@SuppressWarnings("unchecked")
		List<CargaArchivo> resultados = query.getResultList();
		
		return resultados.isEmpty() ? null : resultados.get(0);
	}
}