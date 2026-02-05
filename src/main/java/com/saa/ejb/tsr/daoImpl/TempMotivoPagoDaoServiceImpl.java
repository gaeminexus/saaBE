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

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.TempMotivoPagoDaoService;
import com.saa.model.tsr.TempMotivoPago;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion TempMotivoPagoDaoService.
 */
@Stateless
public class TempMotivoPagoDaoServiceImpl extends EntityDaoImpl<TempMotivoPago> implements TempMotivoPagoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"tempPago",
							"plantilla",
							"detallePlantilla",
							"descripcion",
							"valor"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.TempMotivoPagoDaoService#eliminaMotivoPagoByIdPago(java.lang.Long)
	 */
	public void eliminaMotivoPagoByIdPago(Long idTempPago) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaMotivoPagoByIdPago con id de pago : " + idTempPago);
		Query query = em.createQuery (" delete b" +
									  " from   TempMotivoPago b " +
									  " where  b.tempPago.codigo = :idTempPago");
		query.setParameter("idTempPago", idTempPago);
		try {
			query.executeUpdate();
		} catch (Exception e) {
			throw new IncomeException("ERROR EN eliminaMotivoPagoByIdPago: " + e.getCause());
		}		
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<TempMotivoPago> selectByIdTempPago(Long idTempPago) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdTempPago con id : " + idTempPago);
		Query query = em.createQuery(" select   b " +
									 " from     TempMotivoPago b " +
									 " where    b.tempPago.codigo = :idTempPago");
		query.setParameter("idTempPago", idTempPago);
		return query.getResultList();
	}

}
