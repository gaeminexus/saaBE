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
import com.saa.ejb.tesoreria.dao.AuxDepositoCierreDaoService;
import com.saa.model.tesoreria.AuxDepositoCierre;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion AuxDepositoCierreDaoService.
 */
@Stateless
public class AuxDepositoCierreDaoServiceImpl extends EntityDaoImpl<AuxDepositoCierre> implements AuxDepositoCierreDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"cierreCaja",
							"usuarioPorCaja",
							"montoEfectivo",
							"montoCheque",
							"seleccionado",
							"montoDeposito",
							"montoTotalCierre",
							"fechaCierre",
							"nombreCaja"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoCierreDaoService#selectByUsuarioCaja(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<AuxDepositoCierre> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo selectByUsuarioCaja con idUsuarioCaja : " + idUsuarioCaja );
		Query query = em.createQuery (" select b " +
									  " from   AuxDepositoCierre b " +
									  " where  b.usuarioPorCaja.codigo = :idUsuarioCaja " +
									  "		   and b.seleccionado = 1");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);		
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoCierreDaoService#selectCierreCajaByIdUsuario(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<AuxDepositoCierre> selectCierreCajaByIdUsuario(Long idUsuario) throws Throwable {
		System.out.println("Ingresa al Metodo selectCierreCajaByIdUsuario con idUsuario : " + idUsuario);
		Query query = em.createQuery(" select b " +
									 " from   AuxDepositoCierre b " +
									 " where  b.usuarioPorCaja = :idUsuario" +
									 "        and seleccionado = :estado");
		query.setParameter("idUsuario", idUsuario);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}

	public void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaPorUsuarioCaja con idUsuarioCaja: " + idUsuarioCaja);
		Query query = em.createQuery (" delete b " +
				  					  " from   AuxDepositoCierre b " +
				  					  " where  b.usuarioPorCaja.codigo = :idUsuarioCaja");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		
		try {
			query.executeUpdate();
		} catch (Exception e) {
			throw new IncomeException("Erro en el metodo eliminaPorUsuarioCaja(AuxDepositoCierre): " + e.getCause());
		}
	}	
	
}
