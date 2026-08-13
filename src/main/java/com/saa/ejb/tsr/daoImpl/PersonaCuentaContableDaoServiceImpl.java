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
							"personaRol",
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
									 " where  b.personaRol.titular.codigo = :idPersona " +
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

	/* (non-Javadoc)
	 * @see com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService#selectByTitularRolTipoCuenta(java.lang.Long, java.lang.Long, int, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<PersonaCuentaContable> selectByTitularRolTipoCuenta(Long idEmpresa, Long codigoTitular,
			int rolPersona, Long tipoCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByTitularRolTipoCuenta con titular: " + codigoTitular
				+ ", empresa: " + idEmpresa + ", rol: " + rolPersona + ", tipoCuenta: " + tipoCuenta);

		Query query = em.createQuery(" select pcc " +
									 " from   PersonaCuentaContable pcc " +
									 " join   pcc.personaRol pr " +
									 " where  pr.titular.codigo = :titular " +
									 "        and pcc.tipoCuenta = :tipoCuenta " +
									 "        and pcc.empresa.codigo = :idEmpresa " +
									 "        and pr.rubroRolPersonaH = :rolPersona");
		query.setParameter("titular", codigoTitular);
		query.setParameter("tipoCuenta", tipoCuenta);
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("rolPersona", Long.valueOf(rolPersona));
		List<PersonaCuentaContable> personaCuentaContables = query.getResultList();

		if (personaCuentaContables.isEmpty()) {
			// Compatibilidad con datos antiguos sin rubroRolPersonaH poblado.
			// Sólo se llega aquí si el titular NO tiene ninguna cuenta bajo el rol
			// pedido, así que no puede devolver la cuenta del otro rol cuando ambos
			// existen.
			Query querySinRol = em.createQuery(" select pcc " +
											   " from   PersonaCuentaContable pcc " +
											   " join   pcc.personaRol pr " +
											   " where  pr.titular.codigo = :titular " +
											   "        and pcc.tipoCuenta = :tipoCuenta " +
											   "        and pcc.empresa.codigo = :idEmpresa");
			querySinRol.setParameter("titular", codigoTitular);
			querySinRol.setParameter("tipoCuenta", tipoCuenta);
			querySinRol.setParameter("idEmpresa", idEmpresa);
			personaCuentaContables = querySinRol.getResultList();
			if (!personaCuentaContables.isEmpty()) {
				System.err.println("⚠ El titular " + codigoTitular + " no tiene cuenta contable "
						+ "(tipoCuenta=" + tipoCuenta + ") bajo el rol " + rolPersona
						+ "; se usa la cuenta sin filtro de rol. Revise "
						+ "PersonaRol.rubroRolPersonaH (PRRLRZZA): debe valer 1=Cliente o 2=Proveedor.");
			}
		}
		return personaCuentaContables;
	}

}
