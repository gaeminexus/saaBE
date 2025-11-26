/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.GrupoProductoCobroDaoService;
import com.saa.model.cxc.GrupoProductoCobro;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion GrupoProductoCobroDaoService. 
 */
@Stateless
public class GrupoProductoCobroDaoServiceImpl extends EntityDaoImpl<GrupoProductoCobro>  implements GrupoProductoCobroDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.parametrizacion.ejb.dao.GrupoProductoCobroDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) GrupoProductoCobro");
		return new String[]{"codigo",
							"nombre",
							"rubroTipoGrupoP",
							"rubroTipoGrupoH",
							"planCuenta",
							"estado",
							"empresa",
							"productoCobros"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.cxc.ejb.dao.GrupoProductoCobroDaoService#recuperaConHijos(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public GrupoProductoCobro recuperaConHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo recuperaConHijos de GrupoProductoCobro: " + id);
		Query query = null;
		//CREA LA VARIABLE STRING QUE CONTIENE LA SENTENCIA WHERE
		query = em.createQuery(" from   GrupoProductoCobro " +
							   " where  codigo = :codigo");
		query.setParameter("codigo", id);
		GrupoProductoCobro grupoProductoCobro = (GrupoProductoCobro)query.getSingleResult();
		System.out.println("Antes de recuperar detalles: " + id);
		// RECUPERA HIJOS PRODUCTO PAGOS
		query = em.createQuery(" select   b.productoCobros " +
							   " from     GrupoProductoCobro b " +
							   " where    b.codigo = :id");
		query.setParameter("id", id);
		grupoProductoCobro.setProductoCobros(query.getResultList());
		
		return grupoProductoCobro;
	}
	
}