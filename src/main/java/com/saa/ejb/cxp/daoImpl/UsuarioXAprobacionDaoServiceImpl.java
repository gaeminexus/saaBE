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
import com.saa.ejb.cxp.dao.UsuarioXAprobacionDaoService;
import com.saa.model.cxp.UsuarioXAprobacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion UsuarioXAprobacionDaoService. 
 */
@Stateless
public class UsuarioXAprobacionDaoServiceImpl extends EntityDaoImpl<UsuarioXAprobacion>  implements UsuarioXAprobacionDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.UsuarioXAprobacionDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) UsuarioXAprobacion");
		return new String[]{"codigo",
							"aprobacionXMonto",
							"usuario",
							"fechaIngreso"};
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.UsuarioXAprobacionDaoService#deleteByEmpresa(java.lang.Long)
	 */
	public void deleteByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("UsuarioXAprobacionDao deleteByEmpresa con idEmpresa: " + idEmpresa);
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		Query query = em.createQuery(" Delete   from UsuarioXAprobacion b" +
							   		 " where  	b.aprobacionXMonto.codigo IN (select   d.codigo" +
							   		 "										  from     AprobacionXMonto d" +
							   		 "										  where    d.montoAprobacion.empresa.codigo = :idEmpresa)");
		query.setParameter("idEmpresa", idEmpresa);
		query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.UsuarioXAprobacionDaoService#selectByAprobacionXMonto(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<UsuarioXAprobacion> selectByAprobacionXMonto(
			Long idAprobacionXMonto) throws Throwable {
		System.out.println("Ingresa a selectByAprobacionXMonto de idAprobacionXMonto: " + idAprobacionXMonto);
		Query query = null;
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		query = em.createQuery(" from   UsuarioXAprobacion " +
							   " where  aprobacionXMonto.codigo = :idAprobacionXMonto");
		query.setParameter("idAprobacionXMonto", idAprobacionXMonto);
		return query.getResultList();
	}
	
}