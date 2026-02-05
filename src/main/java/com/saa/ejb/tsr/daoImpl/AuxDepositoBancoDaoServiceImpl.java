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
import com.saa.ejb.tsr.dao.AuxDepositoBancoDaoService;
import com.saa.model.tsr.AuxDepositoBanco;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion AuxDepositoBancoDaoService.
 */
@Stateless
public class AuxDepositoBancoDaoServiceImpl extends EntityDaoImpl<AuxDepositoBanco> implements AuxDepositoBancoDaoService {
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"banco",
							"cuentaBancaria",
							"usuarioPorCaja",
							"valor",
							"valorEfectivo",
							"valorCheque"};
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoBancoDaoService#selectIdByUsuarioCajaBancoCuenta(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public AuxDepositoBanco selectIdByUsuarioCajaBancoCuenta(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectIdByUsuarioCajaBancoCuenta con id de usuario por caja: " + idUsuarioCaja + " id de Banco: " + idBanco + " id de cuenta: " + idCuenta);
		Query query = em.createQuery (" select b " +
									  " from     AuxDepositoBanco b " +
									  " where    b.usuarioPorCaja.codigo = :idUsuarioCaja"+
									  " 		 and b.banco.codigo = :banco " +
									  "			 and b.cuentaBancaria.codigo = :cuenta");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		query.setParameter ("banco", idBanco);
		query.setParameter ("cuenta", idCuenta);
		return (AuxDepositoBanco) query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoBancoDaoService#selectByUsuarioCaja(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<AuxDepositoBanco> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al metodo selectByUsuarioCaja con id de usuario por caja: " + idUsuarioCaja);
		Query query = em.createQuery (" select b " +
									  " from   AuxDepositoBanco b " +
									  " where  b.usuarioPorCaja.codigo = :idUsuarioCaja");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);		
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoBancoDaoService#selectRegistroByBanco(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<AuxDepositoBanco> selectRegistroByBanco(Long idBanco, Long codigoCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectRegistroByBanco con idBanco :" + idBanco + ", codigoCuenta " + codigoCuenta);
		Query query = em.createQuery(" select b " +
									 " from   AuxDepositoBanco b " +
									 " where  b.banco = :idBanco" +
									 "		  and b.cuentaBancaria.codigo = :codigoCuenta");
		query.setParameter("idBanco", idBanco);
		query.setParameter("codigoCuenta", codigoCuenta);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoBancoDaoService#selectAuxDepositoBancoByIdUsuario(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<AuxDepositoBanco> selectAuxDepositoBancoByIdUsuario( Long idUsuario) throws Throwable {
		System.out.println("Ingresa al Metodo selectAuxDepositoBancoByIdUsuario con idUsuario: " + idUsuario);
		Query query = em.createQuery(" select b " +
									 " from   AuxDetalleDeposito b " +
									 " where  b.usuarioPorCaja.codigo = :idUsuario");
		query.setParameter("idUsuario", idUsuario);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoBancoDaoService#eliminaPorUsuarioCaja(java.lang.Long)
	 */
	public void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaPorUsuarioCaja con idUsuarioCaja: " + idUsuarioCaja);
		Query query = em.createQuery (" delete b" +
				  					  " from   AuxDepositoBanco b " +
				  					  " where  b.usuarioPorCaja.codigo = :idUsuarioCaja");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		try {
			query.executeUpdate();
		} catch (Exception e) {
			throw new IncomeException("Error en el metodo eliminaPorUsuarioCaja(AuxDepositoBanco): " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoBancoDaoService#eliminaPorUsuarioCajaCuenta(java.lang.Long, java.lang.Long)
	 */
	public void eliminaPorUsuarioCajaCuenta(Long idUsuarioCaja, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaPorUsuarioCaja con idUsuarioCaja: " + idUsuarioCaja);
		Query query = em.createQuery (" delete b" +
				  					  " from   AuxDepositoBanco b " +
				  					  " where  b.usuarioPorCaja.codigo = :idUsuarioCaja"+
				  					  "        and b.cuentaBancaria.codigo = :cuenta");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		query.setParameter("cuenta", idCuenta);
		try {
			query.executeUpdate();
		} catch (Exception e) {
			throw new IncomeException("Error en el metodo eliminaPorUsuarioCaja(AuxDepositoBanco): " + e.getCause());
		}
	}	
	
}
