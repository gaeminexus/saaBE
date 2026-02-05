/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.daoImpl;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.CajaLogicaPorCajaFisicaDaoService;
import com.saa.model.tsr.CajaLogicaPorCajaFisica;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion CajaLogicaPorCajaFisicaDaoService.
 */
@Stateless
public class CajaLogicaPorCajaFisicaDaoServiceImpl extends EntityDaoImpl<CajaLogicaPorCajaFisica> implements CajaLogicaPorCajaFisicaDaoService {	
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"cajaLogica",
							"cajaFisica",
							"estado",
							"fechaIngreso"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CajaLogicaPorCajaFisicaDaoService#recuperaNumeroCajas(java.lang.Long)
	 */
	public Long recuperaNumeroCajas(Long idCajaFisica) throws Throwable {
		System.out.println("Ingresa al metodo recuperaNumeroCajas con caja fisica: " + idCajaFisica);
		Long numeroCajas = 0L;
		Query query = em.createQuery(" select b " +
									 " from   CajaLogicaPorCajaFisica b " +
									 " where  b.cajaFisica.codigo = :cajaFisica " +
									 "        and b.cajaLogica.estado = :estadoCajaLogica " +
									 "        and b.estado = :estado");
		query.setParameter("cajaFisica", idCajaFisica);
		query.setParameter("estadoCajaLogica", Estado.ACTIVO);
		query.setParameter("estado", Estado.ACTIVO);
		try {
			if(query.getResultList().size() > 0)
				numeroCajas = Long.valueOf(query.getResultList().size());
		} catch (PersistenceException e) {
			throw new IncomeException("ERROR AL RECUPERAR NUMERO DE CAJAS LOGICAS EN CAJA " + idCajaFisica + " : " + e.getCause());
		}		
		return numeroCajas;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CajaLogicaPorCajaFisicaDaoService#selectByCajaFisica(java.lang.Long)
	 */
	public CajaLogicaPorCajaFisica selectByCajaFisica(Long idCajaFisica) throws Throwable {
		System.out.println("Ingresa al metodo selectByCajaFisica con caja fisica: " + idCajaFisica);
		CajaLogicaPorCajaFisica cajaLogicaPorCajaFisica = null;
		Query query = em.createQuery(" select b " +
									 " from   CajaLogicaPorCajaFisica b " +
									 " where  b.cajaFisica.codigo = :cajaFisica " +
									 "        and b.cajaLogica.estado = :estadoCajaLogica " +
									 "        and b.estado = :estado");
		query.setParameter("cajaFisica", idCajaFisica);
		query.setParameter("estadoCajaLogica", Estado.ACTIVO);
		query.setParameter("estado", Estado.ACTIVO);
		try {
			cajaLogicaPorCajaFisica = (CajaLogicaPorCajaFisica) query.getSingleResult();
		} catch (PersistenceException e) {
			throw new IncomeException("Error en selectByCajaFisica: " + e.getCause());
		}
		
		return cajaLogicaPorCajaFisica;
	}
}
