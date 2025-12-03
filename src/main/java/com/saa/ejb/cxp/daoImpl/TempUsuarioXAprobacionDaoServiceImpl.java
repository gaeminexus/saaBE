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
import com.saa.ejb.cxp.dao.TempUsuarioXAprobacionDaoService;
import com.saa.model.cxp.TempUsuarioXAprobacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion TempUsuarioXAprobacionDaoService. 
 */
@Stateless
public class TempUsuarioXAprobacionDaoServiceImpl extends EntityDaoImpl<TempUsuarioXAprobacion>  implements TempUsuarioXAprobacionDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.TempUsuarioXAprobacionDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) TempUsuarioXAprobacion");
		return new String[]{"codigo",
							"tempAprobacionXMonto",
							"usuario",
							"fechaIngreso"};
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.TempUsuarioXAprobacionDaoService#selectByTempAprobacionXMonto(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<TempUsuarioXAprobacion> selectByTempAprobacionXMonto(
			Long idTempAprobacionXMonto) throws Throwable {
		System.out.println("Dao selectByTempAprobacionXMonto de idTempAprobacionXMonto: " + idTempAprobacionXMonto);
		Query query = null;
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		query = em.createQuery(" from   TempUsuarioXAprobacion " +
							   " where  tempAprobacionXMonto.codigo = :idTempAprobacionXMonto");
		query.setParameter("idTempAprobacionXMonto", idTempAprobacionXMonto);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.TempUsuarioXAprobacionDaoService#deleteByEmpresa(java.lang.Long)
	 */
	public void deleteByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Dao deleteByEmpresa con idEmpresa: " + idEmpresa);
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		Query query = em.createQuery(" Delete   from TempUsuarioXAprobacion b" +
		   		 					 " where  	b.tempAprobacionXMonto.codigo IN (select   d.codigo" +
		   		 					 "										  	  from     TempAprobacionXMonto d" +
		   		 					 "										  	  where    d.tempMontoAprobacion.empresa.codigo = :idEmpresa)");
		query.setParameter("idEmpresa", idEmpresa);
		query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.cxp.ejb.dao.TempUsuarioXAprobacionDaoService#saveTempUsuarioXAprobacion(com.compuseg.income.pago.ejb.model.TempUsuarioXAprobacion)
	 */
	public TempUsuarioXAprobacion saveTempUsuarioXAprobacion(
			TempUsuarioXAprobacion tempUsuarioXAprobacion) throws Throwable {
		System.out.println("Dao saveTempUsuarioXAprobacion con TempUsuarioXAprobacion: " + tempUsuarioXAprobacion);
		em.persist(tempUsuarioXAprobacion);
		return tempUsuarioXAprobacion;
	}
	
}