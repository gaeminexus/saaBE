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
import com.saa.ejb.tesoreria.dao.CuentaBancariaDaoService;
import com.saa.model.contabilidad.PlanCuenta;
import com.saa.model.tesoreria.CuentaBancaria;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion CuentaBancariaDaoService.
 */
@Stateless
public class CuentaBancariaDaoServiceImpl extends EntityDaoImpl<CuentaBancaria> implements CuentaBancariaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"banco",
							"numeroCuenta",
							"rubroTipoCuentaP",
							"rubroTipoCuentaH",
							"saldoInicial",
							"planCuenta",
							"fechaCreacion",
							"titular",
							"rubroTipoMonedaP",
							"rubroTipoMonedaH",
							"oficialCuenta",
							"telefono1",
							"telefono2",
							"celular",
							"fax",
							"email",
							"direccion",
							"observacion",
							"estado",
							"fechaIngreso",
							"fechaInactivo",
							"cuentaApertura"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CuentaBancariaDaoService#recuperaCuentaContable(java.lang.Long)
	 */
	public PlanCuenta recuperaCuentaContable(Long idCuenta) throws Throwable {
		System.out.println("Ingresa al recuperaCuentaContable con id: " + idCuenta);
		PlanCuenta planCuenta = new PlanCuenta();
		Query query = em.createQuery(" select b " +
									 " from    CuentaBancaria " +
									 " where   codigo = :codigo");
		query.setParameter("codigo", idCuenta);
		CuentaBancaria cuentaBancaria = (CuentaBancaria)query.getSingleResult();
		planCuenta = cuentaBancaria.getPlanCuenta();
		return planCuenta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CuentaBancariaDaoService#recuperaBancoCuenta(java.lang.Long)
	 */
	public CuentaBancaria recuperaBancoCuenta(Long idCuentaBancaria) throws Throwable {
		System.out.println (" Ingresa al Metodo recuperaBancoCuenta con idCuenta : " + idCuentaBancaria);
		Query query = em.createQuery (" select b " +
									  " from   CuentaBancaria b " +
									  " where  b.codigo = :idCuentaBancaria");
		query.setParameter ("idCuentaBancaria", idCuentaBancaria);
		CuentaBancaria cuentaBancaria = (CuentaBancaria) query.getSingleResult();
		return cuentaBancaria;		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CuentaBancariaDaoService#recuperaDatosCuenta(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CuentaBancaria> recuperaBancoCuentaById(Long idCuentaBancaria) throws Throwable {
		System.out.println("Ingresa al Metodo recuperaDatosCuenta con idCuentaBancaria : " + idCuentaBancaria);
		Query query = em.createQuery (" select b " +
									  " from   CuentaBancaria b " +
									  " where  b.codigo = :idCuentaBancaria ");
		query.setParameter("idCuentaBancaria", idCuentaBancaria);
		return query.getResultList();		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CuentaBancariaDaoService#selectByEmpresaSinCuenta(java.lang.Long, java.lang.String, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CuentaBancaria> selectByEmpresaSinCuenta(Long empresa, String numeroCuenta, Long estado) throws Throwable {
		System.out.println("Ingresa al Metodo selectByEmpresaSinCuenta con empresa : " + empresa + ", numeroCuenta: " + numeroCuenta + ", estado: " + estado);
		Query query = em.createQuery (" select b " +
									  " from   CuentaBancaria b " +
									  " where  b.banco.empresa.codigo = :empresa " +
									  "		   and   numeroCuenta != :numeroCuenta " +
									  "		   and   estado = :estado");
		query.setParameter("empresa", empresa);
		query.setParameter("numeroCuenta", numeroCuenta);
		query.setParameter("estado", estado);
		return query.getResultList();		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CuentaBancariaDaoService#selectBancoNumeroByIdCuenta(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CuentaBancaria> selectBancoNumeroByIdCuenta(Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectBancoNumerobyIdCuenta con idCuenta: " + idCuenta);
		Query query = em.createQuery(" select b " +
				          			 " from   Cuentabancaria" +
									 " where  b.numeroCuenta = :idCuenta");
		query.setParameter("idCuenta", idCuenta);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CuentaBancariaDaoService#selectConciliacionByDescuadre(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CuentaBancaria> selectConciliacionByDescuadre(Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectConciliacionByDescuadre con idCuenta: " + idCuenta);
		Query query = em.createQuery(" select b " +
									 " from   CuentaBancaria b " +
									 " where  b.codigo = :idCuenta");
		query.setParameter("idCuenta", idCuenta);
		return query.getResultList();
	}
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CuentaBancariaDaoService#selectSaldosByBancoCta(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CuentaBancaria> selectSaldosByBancoCta(Long idEmpresa, Long idBanco, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectConciliacionByDescuadre con idBanco: " + idBanco + " idCuenta: " + idCuenta);		
		String strQuery =" select b " +
						 "from   CuentaBancaria b";
		strQuery += " where b.banco.empresa.codigo = :idEmpresa";
		if(idBanco!=0){
			strQuery += " and b.banco.codigo = :idBanco";
		}				
		if(idCuenta!=0){
			strQuery += " and b.codigo = :idCuenta";
		}		
		strQuery += " order by b.banco.nombre, b.numeroCuenta";
		Query query = em.createQuery(strQuery);
		query.setParameter("idEmpresa", idEmpresa);
		if(idBanco!=0){
			query.setParameter("idBanco", idBanco);
		}				
		if(idCuenta!=0){
			query.setParameter("idCuenta", idCuenta);
		}		
		return query.getResultList();
	}
	
}