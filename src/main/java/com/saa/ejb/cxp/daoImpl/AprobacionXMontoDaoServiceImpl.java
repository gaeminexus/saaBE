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
import com.saa.ejb.cxp.dao.AprobacionXMontoDaoService;
import com.saa.model.cxp.AprobacionXMonto;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion AprobacionXMontoDaoService. 
 */
@Stateless
public class AprobacionXMontoDaoServiceImpl extends EntityDaoImpl<AprobacionXMonto>  implements AprobacionXMontoDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.AprobacionXMontoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) AprobacionXMonto");
		return new String[]{"codigo",
							"montoAprobacion",
							"nombreNivel",
							"estado",
							"fechaIngreso",
							"usuarioIngresa",
							"ordenAprobacion",
							"seleccionaBanco"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.AprobacionXMontoDaoService#recuperaConHijos(java.lang.Long)
	 */
	public AprobacionXMonto recuperaConHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaConHijos de aprobacionXMonto: " + id);
		Query query = null;
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		query = em.createQuery(" from   AprobacionXMonto " +
							   " where  codigo = :codigo");
		query.setParameter("codigo", id);
		AprobacionXMonto aprobacionXMonto = (AprobacionXMonto)query.getSingleResult();
		// RECUPERA HIJOS DETALLE ASIENTO
		query = em.createQuery(" select   b.usuarioXAprobacions " +
							   " from     AprobacionXMonto b " +
							   " where    b.codigo = :id");
		query.setParameter("id", id);
		aprobacionXMonto.setUsuarioXAprobacions(query.getResultList());
		
		return aprobacionXMonto;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.AprobacionXMontoDaoService#selectByMontoAprobacion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<AprobacionXMonto> selectByMontoAprobacion(Long idMontoAprobacion)
			throws Throwable {
		System.out.println("Ingresa a selectByMontoAprobacion de idMontoAprobacion: " + idMontoAprobacion);
		Query query = null;
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		query = em.createQuery(" from   AprobacionXMonto " +
							   " where  montoAprobacion.codigo = :idMontoAprobacion");
		query.setParameter("idMontoAprobacion", idMontoAprobacion);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.AprobacionXMontoDaoService#saveAprobacionXMonto(com.compuseg.income.pago.ejb.model.AprobacionXMonto)
	 */
	public AprobacionXMonto saveAprobacionXMonto(
			AprobacionXMonto aprobacionXMonto) throws Throwable {
		System.out.println("Ingresa al metodo saveAprobacionXMonto de aprobacionXMonto");
		em.persist(aprobacionXMonto);
		return aprobacionXMonto;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.AprobacionXMontoDaoService#deleteByEmpresa(java.lang.Long)
	 */
	public void deleteByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("AprobacionXMontoDaoService deleteByEmpresa con idEmpresa: " + idEmpresa);
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		Query query = em.createQuery(" Delete   from AprobacionXMonto b" +
							   		 " where  	b.montoAprobacion IN (select   d.codigo" +
							   		 "								  from     MontoAprobacion d" +
							   		 "								  where    d.empresa.codigo = :idEmpresa)");
		query.setParameter("idEmpresa", idEmpresa);
		query.executeUpdate();
	}
	
}