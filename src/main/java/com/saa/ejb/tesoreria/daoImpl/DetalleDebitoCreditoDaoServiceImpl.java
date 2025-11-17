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
import com.saa.ejb.tesoreria.dao.DetalleDebitoCreditoDaoService;
import com.saa.model.tesoreria.DetalleDebitoCredito;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion DetalleDebitoCreditoDaoService.
 */
@Stateless
public class DetalleDebitoCreditoDaoServiceImpl extends EntityDaoImpl<DetalleDebitoCredito> implements DetalleDebitoCreditoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"debitoCredito",
							"detallePlantilla",
							"descripcion",
							"valor"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.DetalleDebitoCreditoDaoService#selectDetalleDebitoByIdDebitoBancario(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleDebitoCredito> selectDetalleDebitoByIdDebitoBancario( Long idDebitoBancario) throws Throwable {
		System.out.println("Ingresa al Metodo selectDetalleDebitoByIdDebitoBancario con idDebitoBancario: " + idDebitoBancario);
		Query query = em.createQuery(" select b " +
									 " from   DetalleDebitoCredito b " +
									 " where  b.debitoBancario = :idDebitoBancario ");
		query.setParameter("idDebitoBancario", idDebitoBancario );		
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleDebitoCredito> selectByIdDebitoCredito(Long idDebitoCredito) throws Throwable {
		System.out.println("Ingresa al Metodo selectByIdDebitoCredito con idDebitoCredito: " + idDebitoCredito);
		Query query = em.createQuery(" select b " +
									 " from   DetalleDebitoCredito b " +
									 " where  b.debitoCredito = :idDebitoCredito ");
		query.setParameter("idDebitoCredito", idDebitoCredito );		
		return query.getResultList();
	}

}