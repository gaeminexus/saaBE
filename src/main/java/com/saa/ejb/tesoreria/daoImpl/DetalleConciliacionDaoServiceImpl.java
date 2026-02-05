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
import com.saa.ejb.tesoreria.dao.DetalleConciliacionDaoService;
import com.saa.model.tsr.DetalleConciliacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion DetalleConciliacionDaoService.
 */
@Stateless
public class DetalleConciliacionDaoServiceImpl extends EntityDaoImpl<DetalleConciliacion> implements DetalleConciliacionDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"conciliacion",
							"descripcion",
							"asiento",
							"valor",
							"conciliado",
							"numeroCheque",
							"rubroTipoMovimientoP",
							"rubroTipoMovimientoH",
							"estado",
							"numeroAsiento",
							"fechaRegistro",
							"idMovimiento",
							"cheque",
							"detalleDeposito",
							"periodo",
							"numeroMes",
							"numeroAnio",
							"rubroOrigenP",
							"rubroOrigenH"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.DetalleConciliacionDaoService#selectHistoricaConciliacion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleConciliacion> selectHistoricaConciliacion( Long conciliacion) throws Throwable {
		System.out.println("Ingresa al Metodo selectHistoricaConciliacion con conciliacion: " + conciliacion);
		Query query = em.createQuery(" select b " +
									 " from   DetalleConciliacion b " +
									 " where  b.conciliacion = :conciliacion");
		query.setParameter("conciliacion", conciliacion);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.DetalleConciliacionDaoService#selectIdByConciliacion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleConciliacion> selectByIdConciliacion(Long idConciliacion) throws Throwable {
		System.out.println("Ingresa al Metodo selectIdByConciliacion con: " + idConciliacion);
		Query query = em.createQuery(" select b " +
									 " from   DetalleConciliacion b " +
									 " where  b.codigo = :idConciliacion");
		query.setParameter("idConciliacion", idConciliacion);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.DetalleConciliacionDaoService#deleteByIdConciliacion(java.lang.Long)
	 */
	public void deleteByIdConciliacion(Long idConciliacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByIdConciliacion de idConciliacion: " + idConciliacion);
		Query query = em.createQuery(" delete from   DetalleConciliacion b " +
				 					 " where  b.conciliacion.codigo = :idConciliacion ");
		query.setParameter("idConciliacion", idConciliacion);
		query.executeUpdate();
	}

}
