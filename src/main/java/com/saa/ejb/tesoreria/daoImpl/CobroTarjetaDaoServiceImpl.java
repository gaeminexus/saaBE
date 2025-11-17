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
import com.saa.ejb.tesoreria.dao.CobroTarjetaDaoService;
import com.saa.model.tesoreria.CobroTarjeta;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion CobroTarjetaDaoService.
 */

@Stateless
public class CobroTarjetaDaoServiceImpl extends EntityDaoImpl<CobroTarjeta> implements CobroTarjetaDaoService {

	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"cobro",
							"numero",
							"valor",
							"numeroVoucher",
							"fechaCaducidad",
							"detallePlantilla"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CobroTarjetaDaoService#selectSumaByCobro(java.lang.Long)
	 */
	public Double selectSumaByCobro(Long idCobro) throws Throwable {
		System.out.println("Ingresa al metodo selectSumaByCobro con id cobro: "+idCobro);
		Query query = em.createQuery(" SELECT   SUM(t.valor) " +
									 " FROM     CobroTarjeta t " +
									 " WHERE    t.cobro.codigo = :cobro");
		query.setParameter("cobro" ,idCobro);
		Double resultado = 0.0;
		if(query.getSingleResult() != null)
			resultado = Double.valueOf(query.getSingleResult().toString()); 
		return resultado;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CobroTarjeta> selectByIdCobro(Long idCobro) throws Throwable {
		System.out.println("CobroTarjeta - selectByIdCobro con id cobro: "+idCobro);
		Query query = em.createQuery(" SELECT   b " +
									 " FROM     CobroTarjeta b " +
									 " WHERE    b.cobro.codigo = :idCobro");
		query.setParameter("idCobro" ,idCobro);
		return query.getResultList();
	}
}