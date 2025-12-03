/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxp.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.MontoAprobacionDaoService;
import com.saa.model.cxp.MontoAprobacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion MontoAprobacionDaoService. 
 */
@Stateless
public class MontoAprobacionDaoServiceImpl extends EntityDaoImpl<MontoAprobacion>  implements MontoAprobacionDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.MontoAprobacionDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) MontoAprobacion");
		return new String[]{"codigo",
							"valorDesde",
							"valorHasta",
							"fechaIngreso",
							"usuarioIngresa",
							"empresa"};
	}
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.MontoAprobacionDaoService#saveMontoAprobacion(com.compuseg.income.pago.ejb.model.MontoAprobacion)
	 */
	public MontoAprobacion saveMontoAprobacion(MontoAprobacion montoAprobacion)
			throws Throwable {
		System.out.println("Ingresa al metodo saveMontoAprobacion de MontoAprobacion");
		em.persist(montoAprobacion);
		return montoAprobacion;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.MontoAprobacionDaoService#deleteByEmpresa(java.lang.Long)
	 */
	public void deleteByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("MontoAprobacionDaoService deleteByEmpresa con idEmpresa: " + idEmpresa);
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		Query query = em.createQuery(" Delete   from MontoAprobacion " +
							   		 " where  	empresa.codigo = :idEmpresa");
		query.setParameter("idEmpresa", idEmpresa);
		query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.MontoAprobacionDaoService#selectValoresByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("rawtypes")
	public List selectValoresByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Dao selectValoresByEmpresa con idEmpresa: " + idEmpresa);
		Query query = em.createQuery(" select   t.valorDesde, t.valorHasta" +
									 " from     MontoAprobacion t "+
									 " where    t.empresa.codigo = :idEmpresa ");
		query.setParameter("idEmpresa", idEmpresa);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.MontoAprobacionDaoService#selectByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<MontoAprobacion> selectByEmpresa(Long idEmpresa)
			throws Throwable {
		System.out.println("Dao selectByEmpresa con idEmpresa: " + idEmpresa);
		Query query = em.createQuery(" from     MontoAprobacion t "+
				 					 " where    t.empresa.codigo = :idEmpresa ");
		query.setParameter("idEmpresa", idEmpresa);
		return query.getResultList();
	}
	
}