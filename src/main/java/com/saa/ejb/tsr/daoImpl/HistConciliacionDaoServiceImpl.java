/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.HistConciliacionDaoService;
import com.saa.model.tsr.HistConciliacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion HistConciliacionDaoService.
 */
@Stateless
public class HistConciliacionDaoServiceImpl extends EntityDaoImpl<HistConciliacion> implements HistConciliacionDaoService {

	//Inicializa persistence context
		@PersistenceContext
		EntityManager em;	
		
		/* (non-Javadoc)
		 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
		 */
		public String[] obtieneCampos() {
			System.out.println("Ingresa al metodo (campos) Ambito");
			return new String[]{"codigo",
								"idPeriodo",
								"usuario",
								"fecha",
								"estado",
								"cuentaBancaria",
								"inicialSistema",
								"depositoSistema",
								"creditoSistema",
								"chequeSistema",
								"debitoSistema",
								"finalSistema",
								"saldoEstadoCuenta",
								"depositoTransito",
								"chequeTransito",
								"creditoTransito",
								"debitoTransito",
								"saldoBanco",
								"empresa",
								"transferenciaDebitoTransito",
								"transferenciaCreditoTransito",
								"transferenciaDebitoSistema",
								"transferenciaCreditoSistema",
								"idConciliacionOrigen"};
		}
		
		/* (non-Javadoc)
		 * @see com.compuseg.income.tesoreria.ejb.dao.HistConciliacionDaoService#selectByCuentaPeriodoConciliacion(java.lang.Long, java.lang.Long, java.lang.Long)
		 */
		public HistConciliacion selectByCuentaPeriodoConciliacion(
				Long idCuenta, Long idPeriodo, Long idConciliacionOrigen)
				throws Throwable {
			System.out.println("Ingresa a selectByCuentaPeriodoConciliacion con idCuenta: " + idCuenta 
					 + ", idPeriodo: " + idPeriodo + ", idConciliacionOrigen: " + idConciliacionOrigen);
			Query query = em.createQuery(" select b " +
										 " from   HistConciliacion b " +
								   		 " where  b.idPeriodo = :idPeriodo " + 
										 " 		  and   b.cuentaBancaria.codigo = :idCuenta " + 
										 " 		  and   b.idConciliacionOrigen = :idConciliacionOrigen ");
			query.setParameter("idPeriodo", idPeriodo);
			query.setParameter("idCuenta", idCuenta);
			query.setParameter("idConciliacionOrigen", idConciliacionOrigen);
			return (HistConciliacion)query.getSingleResult();
		}
		
	}
