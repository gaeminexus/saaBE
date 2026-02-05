/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.model.tsr.PersonaCuentaContable;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion PersonaCuentaContableDaoService.
 */
@Stateless
public class PersonaCuentaContableDaoServiceImpl extends EntityDaoImpl<PersonaCuentaContable> implements PersonaCuentaContableDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"persona",
							"empresa",
							"tipoCuenta",
							"tipoPersona",
							"planCuenta"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.PersonaCuentaContableDaoService#selectByPersonaTipoCuenta(java.lang.Long, java.lang.Long, int, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<PersonaCuentaContable> selectByPersonaTipoCuenta(Long idEmpresa, Long idPersona, int rolPersona, Long tipoCuenta) throws Throwable {
		System.out.println("Ingresa al stelectByCodigoPersona con id: " + idPersona);
		Query query = em.createQuery(" select b " +
									 " from   PersonaCuentaContable b " +
									 " where  b.persona.codigo = :idPersona " +
									 "        and b.tipoPersona = :rolPersona " +
									 "        and b.tipoCuenta = :tipoCuenta"+
									 "        and b.empresa.codigo = :idEmpresa");
		query.setParameter("idPersona", idPersona);
		query.setParameter("rolPersona", Long.valueOf(rolPersona));
		query.setParameter("tipoCuenta", tipoCuenta);
		query.setParameter("idEmpresa", idEmpresa);
		List<PersonaCuentaContable> personaCuentaContables = query.getResultList();
		return personaCuentaContables;
	}

}
