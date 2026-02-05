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
import com.saa.ejb.tsr.dao.CobroTransferenciaDaoService;
import com.saa.model.tsr.CobroTransferencia;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion CobroTransferenciaDaoService.
 */
@Stateless
public class CobroTransferenciaDaoServiceImpl extends EntityDaoImpl<CobroTransferencia> implements CobroTransferenciaDaoService {

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
							"cuentaOrigen",
							"numeroTransferencia",
							"banco",
							"cuentaBancaria",
							"cuentaDestino",
							"valor"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CobroTransferenciaDaoService#selectSumaByCobro(java.lang.Long)
	 */
	public Double selectSumaByCobro(Long idCobro) throws Throwable {
		System.out.println("Ingresa al metodo selectSumaByCobro con id cobro: "+idCobro);
		Query query = em.createQuery(" SELECT   SUM(t.valor) " +
									 " FROM     CobroTransferencia t " +
									 " WHERE t.cobro.codigo = :cobro");
		query.setParameter("cobro" ,idCobro);
		Double resultado = 0.0;
		if(query.getSingleResult() != null)
			resultado = Double.valueOf(query.getSingleResult().toString()); 
		return resultado;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CobroTransferencia> selectByIdCobro(Long idCobro) throws Throwable {
		System.out.println("CobroTransferencia - selectByIdCobro con id cobro: "+idCobro);
		Query query = em.createQuery(" SELECT   b " +
									 " FROM     CobroTransferencia b " +
									 " WHERE    b.cobro.codigo = :idCobro");
		query.setParameter("idCobro" ,idCobro);
		return query.getResultList();
	}

}
