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
import com.saa.ejb.tesoreria.dao.DetalleCierreDaoService;
import com.saa.model.tsr.DetalleCierre;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion DetalleCierreDaoService.
 */
@Stateless
public class DetalleCierreDaoServiceImpl extends EntityDaoImpl<DetalleCierre> implements DetalleCierreDaoService {

	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"cierreCaja",
							"cobro",
							"nombreCliente",
							"fechaCobro",
							"valorEfectivo",
							"valorCheque",
							"valorTarjeta",
							"valorTransferencia",
							"valorRetencion",
							"valorTotal"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.DetalleCierreDaoService#selectByCierreCaja(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleCierre> selectByCierreCaja(Long idCierreCaja) throws Throwable {
		System.out.println("Ingresa al metodo selectByCierreCaja con id cobro: "+idCierreCaja);
		Query query = em.createQuery( " select b " +
									  " from     DetalleCierre b " +
					   				  " where    b.cierreCaja.codigo = :id");
		query.setParameter("id", idCierreCaja);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.DetalleCierreDaoService#selectDistinctCajaLogicaByCierreCaja(java.lang.Long)
	 */
	@SuppressWarnings({"rawtypes" })
	public List selectDistinctCajaLogicaByCierreCaja(Long idCierreCaja) throws Throwable {
		System.out.println("Ingresa al metodo selectDistinctCajaLogicaByCierreCaja con id cobro: "+idCierreCaja);
		Query query = em.createQuery( " select 	 d.cobro.cajaLogica.codigo, d.cobro.cajaLogica.nombre, sum(d.valorEfectivo + d.valorCheque)"+
									  " from 	 DetalleCierre d"+ 
									  " where 	 d.cierreCaja.codigo = :id"+
									  " group by d.cobro.cajaLogica.codigo, d.cobro.cajaLogica.nombre");
		query.setParameter("id", idCierreCaja);
		return query.getResultList();
	}
	
	
}
