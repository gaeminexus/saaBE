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
import com.saa.ejb.tsr.dao.ChequeraDaoService;
import com.saa.model.tsr.Chequera;
import com.saa.rubros.EstadoChequera;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion ChequeraDaoService.
 */
@Stateless
public class ChequeraDaoServiceImpl extends EntityDaoImpl<Chequera> implements ChequeraDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"fechaSolicitud",
							"fechaEntrega",
							"numeroCheques",
							"comienza",
							"finaliza",
							"cuentaBancaria",
							"rubroEstadoChequeraP",
							"rubroEstadoChequeraH"};
	}

	@SuppressWarnings("unchecked")
	public Long selectMaxFinalizaByCuenta(Long idCuentaBancaria) throws Throwable {
		System.out.println("Ingresa al Metodo selectMaxFinalizaByCuenta con cuenta: " + idCuentaBancaria);
		Query query = em.createQuery(
				" select max(c.finaliza) from Chequera c " +
				" where c.cuentaBancaria.codigo = :idCuenta " +
				" and (c.rubroEstadoChequeraH is null or c.rubroEstadoChequeraH <> :anulada)");
		query.setParameter("idCuenta", idCuentaBancaria);
		query.setParameter("anulada", Long.valueOf(EstadoChequera.ANULADA));
		return (Long) query.getSingleResult();
	}

	public boolean existeSolape(Long idCuentaBancaria, Long comienza, Long finaliza) throws Throwable {
		System.out.println("Ingresa al Metodo existeSolape con cuenta: " + idCuentaBancaria
				+ " | rango: " + comienza + "-" + finaliza);
		Query query = em.createQuery(
				" select count(c) from Chequera c " +
				" where c.cuentaBancaria.codigo = :idCuenta " +
				" and (c.rubroEstadoChequeraH is null or c.rubroEstadoChequeraH <> :anulada) " +
				" and c.comienza <= :finaliza and c.finaliza >= :comienza");
		query.setParameter("idCuenta", idCuentaBancaria);
		query.setParameter("anulada", Long.valueOf(EstadoChequera.ANULADA));
		query.setParameter("comienza", comienza);
		query.setParameter("finaliza", finaliza);
		Long total = (Long) query.getSingleResult();
		return total != null && total > 0;
	}

	@SuppressWarnings("unchecked")
	public List<Chequera> selectByCuentaBancaria(Long idCuentaBancaria) throws Throwable {
		System.out.println("Ingresa al Metodo selectByCuentaBancaria con cuenta: " + idCuentaBancaria);
		Query query = em.createQuery(
				" select c from Chequera c where c.cuentaBancaria.codigo = :idCuenta order by c.comienza");
		query.setParameter("idCuenta", idCuentaBancaria);
		return query.getResultList();
	}

}
