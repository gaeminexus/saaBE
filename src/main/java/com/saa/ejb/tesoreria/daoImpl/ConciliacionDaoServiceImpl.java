/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tesoreria.dao.ConciliacionDaoService;
import com.saa.model.tesoreria.Conciliacion;
import com.saa.rubros.EstadosConciliacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion ConciliacionDaoService.
 */
@Stateless
public class ConciliacionDaoServiceImpl extends EntityDaoImpl<Conciliacion> implements ConciliacionDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"idPeriodo",
							"usuario",
							"fecha",
							"cuentaBancaria",
							"inicialSistema",
							"depositoSistema",
							"creditoSistema",
							"chequeSistema",
							"debitoSistema",
							"finalSistema",
							"saldoEstadoCuenta",
							"depositoTransito",
							"chequeTransito",
							"creditoTransito",
							"debitoTransito",
							"saldoBanco",
							"empresa",
							"rubroEstadoP",
							"rubroEstadoH",
							"transferenciaDebitoTransito",
							"transferenciaCreditoTransito",
							"transferenciaDebitoSistema",
							"transferenciaCreditoSistema"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.ConciliacionDaoService#selectRegistros(java.lang.Long, java.lang.Long)
	 */	
	public Long selectValidacion(Long idCuentaBancaria, Long idPeriodo) throws Throwable {
		System.out.println(" Ingresa al Metodo selectRegistros con idCuentaBancaria, : " + idCuentaBancaria +  ", idPeriodo : " + idPeriodo) ;
		Query query = em.createQuery (" select b " +
				  					  " from   Conciliacion b " +
									  " where  b.cuentaBancaria.codigo = :idCuentaBancaria " +
									  "		   and b.idPeriodo = :idPeriodo " +
									  "		   and b.rubroEstadoH =:estado");
		query.setParameter("idCuentaBancaria", idCuentaBancaria);
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("estado", Long.valueOf(EstadosConciliacion.CONCILIADO));
		return Long.valueOf(query.getResultList().size());			
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.ConciliacionDaoService#selectRegistrosByConciliacion(java.lang.Long, java.lang.Long)
	 */
	public int selectRegistrosByConciliacion(Long idPeriodo, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectRegistrosByConciliacion con idPeriodo: " + idPeriodo + ", idCuenta" + idCuenta);
		Query query = em.createQuery(" select b " +
									 " from   Conciliacion b" +
									 " where  b.idPeriodo = :idPeriodo" +
									 "        and b.cuentaBancaria = :idCuenta" +
									 "		  and b.rubroEstadoH = estado");
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("estado", Long.valueOf(EstadosConciliacion.CONCILIADO));
		return query.getResultList().size();
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.ConciliacionDaoService#selectIdConciliacion(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Conciliacion> selectIdConciliacion(Long idPeriodo, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectIdConciliacion con idPeriodo: " + idPeriodo + ", idCuenta" + idCuenta);
		Query query = em.createQuery(" select b " +
				           			 " from   Conciliacion b " +
									 " where  b.idPeriodo = :idPeriodo" +
									 "		  and b.cuentaBancaria = :idCuenta");
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("idCuenta", idCuenta);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria
	 * .ejb.dao.ConciliacionDaoService#selectConciliacionByIdConciliacion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Conciliacion> selectConciliacionByIdConciliacion( Long idConciliacion) throws Throwable {
		System.out.println("Ingresa al Metodo selectConciliacionByIdConciliacion con idConciliacion: " + idConciliacion);
		Query query = em.createQuery(" select b " +
									 " from   Conciliacion b " +
									 " where  b.codigo = :idConciliacion");
		query.setParameter("idConciliacion", idConciliacion);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.ConciliacionDaoService#selectByPeriodoCuentaEstado(java.lang.Long, java.lang.Long, int)
	 */
	public Conciliacion selectByPeriodoCuentaEstado(Long idPeriodo,
			Long idCuenta, int estado) throws Throwable {
		System.out.println("Ingresa al Metodo selectByPeriodoCuentaEstado con idPeriodo: " 
				  + idPeriodo + ", idCuenta" + idCuenta + ", estado: " + estado);
		Query query = em.createQuery(" select b " +
									 " from   Conciliacion b" +
									 " where  b.idPeriodo = :idPeriodo" +
									 "        and   b.cuentaBancaria.codigo = :idCuenta" +
									 "		  and   b.rubroEstadoH = :estado");
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("estado", Long.valueOf(estado));
		return (Conciliacion)query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.ConciliacionDaoService#selectByCuentaPeriodo(java.lang.Long, java.lang.Long)
	 */
	public Conciliacion selectByCuentaPeriodo(Long idCuenta, Long idPeriodo)
			throws Throwable {
		System.out.println("Ingresa al metodo selectByCuentaPeriodo de idCuenta: " + idCuenta + " y idPeriodo: " + idPeriodo);
		Conciliacion conciliacion = new Conciliacion();
		Query query = em.createQuery(" select b " +
									 " from   Conciliacion b" +
									 " where  b.cuentaBancaria = :idCuenta " +
									 "		  and   b.idPeriodo = :idPeriodo");
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("idPeriodo", idPeriodo);
		try {
			conciliacion = (Conciliacion)query.getSingleResult();
		} catch (NoResultException e) {
			conciliacion = null;
		}				
		return conciliacion;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.ConciliacionDaoService#deleteByIdConciliacion(java.lang.Long)
	 */
	public void deleteByIdConciliacion(Long idConciliacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByIdConciliacion de idConciliacion: " + idConciliacion);
		Query query = em.createQuery(" delete from   Conciliacion b" +
				 					 " where  b.codigo = :idConciliacion ");
		query.setParameter("idConciliacion", idConciliacion);
		query.executeUpdate();
	}
	
}