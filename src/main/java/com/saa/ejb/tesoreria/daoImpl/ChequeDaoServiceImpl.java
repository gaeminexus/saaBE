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
import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tesoreria.dao.ChequeDaoService;
import com.saa.model.tesoreria.Cheque;
import com.saa.rubros.EstadoCheque;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion ChequeDaoServices.
 */
@Stateless
public class ChequeDaoServiceImpl extends EntityDaoImpl<Cheque> implements ChequeDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"chequera",
							"numero",
							"egreso",
							"fechaUso",
							"fechaCaduca",
							"fechaAnulacion",
							"rubroEstadoChequeP",
							"rubroEstadoChequeH",
							"fechaImpresion",
							"fechaEntrega",
							"asiento",
							"persona",
							"valor",
							"rubroMotivoAnulacionP",
							"rubroMotivoAnulacionH",
							"beneficiario",
							"idBeneficiario"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.ChequeDaoService#selectMaxCheque(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Cheque> selectMaxCheque(Long cuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectMaxCheque con cuenta:  " + cuenta);
		Query query = em.createQuery(" select   max(b.numero) " +
									 " from     Cheque b " +
									 " where    b.chequera.cuentaBancaria.codigo = :cuenta");
		query.setParameter("cuenta" ,cuenta);
		return query.getResultList();
	}

	public Long selectMinChequeActivo(Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectMinChequeActivo con cuenta:  " + idCuenta);
		Long codigo = 0L;
		Query query = em.createQuery(" Select min(b.codigo) from Cheque b where b.chequera.cuentaBancaria.codigo = :cuenta and rubroEstadoChequeH = :estado");
		query.setParameter("cuenta" ,idCuenta);
		query.setParameter("estado" ,Long.valueOf(EstadoCheque.ACTIVO));
		String respuesta = null;
		try {
			respuesta = query.getSingleResult().toString();
		} catch (Exception e) {
			throw new IncomeException("ERROR AL RECUPERAR ID DE CHEQUE: "+e.getMessage());
		}
		if(respuesta != null)
			codigo = Long.valueOf(respuesta);
		return codigo;
	}

}
