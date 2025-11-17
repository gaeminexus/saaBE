/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tesoreria.dao.CajaLogicaDaoService;
import com.saa.model.contabilidad.PlanCuenta;
import com.saa.model.tesoreria.CajaLogica;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion CajaLogicaDaoService.
 */
@Stateless
public class CajaLogicaDaoServiceImpl extends EntityDaoImpl<CajaLogica> implements CajaLogicaDaoService {
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"grupoCaja",
							"nombre",
							"planCuenta",
							"cuentaContable",
							"fechaIngreso",
							"fechaInactivo",
							"estado"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CajaLogicaDaoService#recuperaCuentaContable(java.lang.Long)
	 */
	public PlanCuenta recuperaCuentaContable(Long idCaja) throws Throwable {
		System.out.println("Ingresa al recuperaCuentaContable con id: " + idCaja);
		PlanCuenta planCuenta = new PlanCuenta();
		Query query = em.createQuery(" select b " +
								     " from   CajaLogica b " + 
								     " where codigo = :codigo");
		query.setParameter("codigo", idCaja);
		CajaLogica cajaLogica = (CajaLogica)query.getSingleResult();
		planCuenta = cajaLogica.getPlanCuenta();
		return planCuenta;
	}
	
}