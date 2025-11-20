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
import com.saa.ejb.tesoreria.dao.CobroChequeDaoService;
import com.saa.model.tesoreria.CobroCheque;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion CobroChequeDaoService.
 */
@Stateless
public class CobroChequeDaoServiceImpl extends EntityDaoImpl<CobroCheque> implements CobroChequeDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"cobro",
							"bancoExterno",
							"numero",
							"valor",
							"detalleDeposito",
							"estado"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CobroChequeDaoService#selectSumaByCobro(java.lang.Long)
	 */
	public Double selectSumaByCobro(Long idCobro) throws Throwable {
		System.out.println("Ingresa al metodo selectSumaByCobro con id cobro: "+idCobro);
		Query query = em.createQuery(" SELECT SUM(t.valor) " +
									 " FROM CobroCheque t " + 
									 " WHERE t.cobro.codigo = :cobro");
		query.setParameter("cobro" ,idCobro);
		Double resultado = 0.0;
		if(query.getSingleResult() != null)
			resultado = Double.valueOf(query.getSingleResult().toString()); 
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CobroChequeDaoService#selectChequesByCobro(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CobroCheque> selectByIdCobro(Long cobro) throws Throwable {
		System.out.println("CobroCheque - selectByIdCobro con cobro: " + cobro);
		Query query = em.createQuery(" select b " +
									 " from   CobroCheque b" +
									 " where  b.cobro.codigo = :cobro");
		query.setParameter("cobro", cobro);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CobroChequeDaoService#selectByDetalleDeposito(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CobroCheque> selectByDetalleDeposito(Long idDetalleDeposito) throws Throwable {
		System.out.println("Ingresa al Metodo selectByDetalleDeposito con id detalle deposito: " + idDetalleDeposito);
		Query query = em.createQuery(" select b " +
									 " from   CobroCheque b " +
									 " where  b.detalleDeposito.codigo = :detalleDeposito ");
		query.setParameter("detalleDeposito", idDetalleDeposito);
		return query.getResultList();
	}
}
