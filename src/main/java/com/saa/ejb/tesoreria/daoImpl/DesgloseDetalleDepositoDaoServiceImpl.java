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
import com.saa.ejb.tesoreria.dao.DesgloseDetalleDepositoDaoService;
import com.saa.model.tsr.DesgloseDetalleDeposito;
import com.saa.rubros.EstadoDeposito;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion DesgloseDetalleDepositoDaoService.
 */
@Stateless
public class DesgloseDetalleDepositoDaoServiceImpl extends EntityDaoImpl<DesgloseDetalleDeposito> implements DesgloseDetalleDepositoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"detalleDeposito",
							"tipo",
							"valor",
							"cobro",
							"bancoExterno",
							"numeroCheque",
							"cobroCheque"};
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.DesgloseDetalleDepositoDaoService#selectDepositosByBancos(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DesgloseDetalleDeposito> selectDepositosByBancos(Long detalleDeposito) throws Throwable {
		System.out.println("Ingresa al Metodo selectDepositosByBancos con detalleDeposito: " + detalleDeposito);
		Query query = em.createQuery(" select b " +
									 " from   DesgloseDetalleDeposito b " +
									 " where  b.detalleDeposito = :detalleDeposito ");
		query.setParameter("detalleDeposito", detalleDeposito);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.DesgloseDetalleDepositoDaoService#selectDetallesRatificadosByDeposito(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DesgloseDetalleDeposito> selectDetallesRatificadosByDeposito(
			Long idDeposito) throws Throwable {
		System.out.println("Ingresa al metodo selectDetallesRatificadosByDeposito con id deposito: " + idDeposito);
		Query query = em.createQuery(" select b " +
									 " from   DesgloseDetalleDeposito" +
									 " where  detalleDeposito.deposito.codigo = :deposito " +
									 " 		  and   detalleDeposito.estado = :estado " +
									 "		  and   cobroCheque is not null");
		query.setParameter("deposito", idDeposito);
		query.setParameter("estado", Long.valueOf(EstadoDeposito.RATIFICADO));
		return query.getResultList();
	}
	
}
