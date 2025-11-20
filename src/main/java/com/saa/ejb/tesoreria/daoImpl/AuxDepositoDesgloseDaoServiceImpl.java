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
import com.saa.ejb.tesoreria.dao.AuxDepositoDesgloseDaoService;
import com.saa.model.tesoreria.AuxDepositoDesglose;
import com.saa.rubros.TipoDesgloseDeposito;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion AuxDepositoDesgloseDaoService.
 */
@Stateless
public class AuxDepositoDesgloseDaoServiceImpl extends EntityDaoImpl<AuxDepositoDesglose> implements AuxDepositoDesgloseDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"tipo",
							"valor",
							"seleccionado",
							"cobro",
							"banco",
							"cuentaBancaria",
							"bancoExterno",
							"numeroCheque",
							"usuarioPorCaja",
							"cobroCheque"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoDesgloseDaoService#selectByUsuarioCaja(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<AuxDepositoDesglose> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo selectByUsuarioCaja con idUsuarioCaja : " + idUsuarioCaja );
		Query query = em.createQuery (" select b " +
									  " from     AuxDepositoDesglose b " +
									  " where    b.usuarioPorCaja.codigo = :idUsuarioCaja");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoDesgloseDaoService#selectEfectivoByUsuarioCaja(java.lang.Long)
	 */
	public AuxDepositoDesglose selectEfectivoByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo selectEfectivoByUsuarioCaja con idUsuarioCaja : " + idUsuarioCaja );
		AuxDepositoDesglose auxDepositoDesglose = null;
		Query query = em.createQuery (" select b " +
									  " from   AuxDepositoDesglose b " +
									  " where  b.usuarioPorCaja.codigo = :idUsuarioCaja"+
									  "        and tipo = :tipo " +
									  "        and banco.codigo is null and cuentaBancaria is null");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		query.setParameter ("tipo", Long.valueOf(TipoDesgloseDeposito.EFECTIVO));
		if(!query.getResultList().isEmpty())
			auxDepositoDesglose = (AuxDepositoDesglose)query.getResultList().get(0);
		return auxDepositoDesglose;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoDesgloseDaoService#eliminaEfectivoRestante(java.lang.Long)
	 */
	public void eliminaEfectivoRestante(Long idUsuarioCaja) throws Throwable {
		System.out.println("ingresa al Metodo eliminaEfectivoRestante con idUsuarioCaja : " + idUsuarioCaja );
		Query query = em.createQuery (" delete b " +
									  " from   AuxDepositoDesglose b " +
									  " where  b.usuarioPorCaja.codigo = :idUsuarioCaja"+
									  "        and b.tipo = :tipo " +
									  "		   and b.banco.codigo is null " +
									  "        and b.cuentaBancaria.codigo is null");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		query.setParameter ("tipo", Long.valueOf(TipoDesgloseDeposito.EFECTIVO));
		try {
			query.executeUpdate();
		} catch (PersistenceException e) {
			throw new IncomeException("Erro en el metodo eliminaEfectivoRestante: " + e.getCause());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoBancoDaoService#selectSumaCheque(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public Double selectSumaCheque(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectSumaCheque con id de usuario por caja: " + idUsuarioCaja + " id de Banco: " + idBanco + " id de cuenta: " + idCuenta);
		Query query = em.createQuery (" select   sum(b.valor) " +
									  " from     AuxDepositoDesglose b " +
									  " where    b.usuarioPorCaja.codigo = :idUsuarioCaja "+
									  " 	     and b.tipo = :tipo " +
									  "          and b.banco.codigo = :banco " +
									  "          and b.cuentaBancaria.codigo = :cuenta");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		query.setParameter ("tipo", Long.valueOf(TipoDesgloseDeposito.CHEQUE));
		query.setParameter ("banco", idBanco);
		query.setParameter ("cuenta", idCuenta);
		Double respuesta = 0.0;
		if(query.getSingleResult() != null)
			respuesta = Double.valueOf(query.getSingleResult().toString());
		return respuesta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoBancoDaoService#selectSumaEfectivo(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public Double selectSumaEfectivo(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectSumaEfectivo1 con id de usuario por caja: " + idUsuarioCaja + " id de Banco: " + idBanco + " id de cuenta: " + idCuenta);
		Query query = em.createQuery (" select   sum(b.valor) " +
									  " from     AuxDepositoDesglose b " +
									  " where    b.usuarioPorCaja.codigo = :idUsuarioCaja "+
									  "          and b.tipo = :tipo " +
									  "          and b.banco.codigo = :banco " +
									  "          and b.cuentaBancaria.codigo = :cuenta");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		query.setParameter ("tipo", Long.valueOf(TipoDesgloseDeposito.EFECTIVO));
		query.setParameter ("banco", idBanco);
		query.setParameter ("cuenta", idCuenta);
		Double respuesta = 0.0;
		if(query.getSingleResult() != null)
			respuesta = Double.valueOf(query.getSingleResult().toString());
		return respuesta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoDesgloseDaoService#selectByUsuarioCajaBancoCuenta(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<AuxDepositoDesglose> selectByUsuarioCajaBancoCuenta( Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByUsuarioCajaBancoCuenta1 con id de usuario por caja: " + idUsuarioCaja + " id de Banco: " + idBanco + " id de cuenta: " + idCuenta);
		String andIdBanco = "";
		String andIdCuenta = "";
		String where =" select b " +
					  " from     AuxDepositoDesglose b " +
		  			  " where    b.usuarioPorCaja.codigo = :idUsuarioCaja ";
		if(idBanco == 0L)
			andIdBanco = "       and b.banco.codigo is null ";
		else
			andIdBanco = "       and b.banco.codigo = :banco ";
		if(idCuenta == 0L)
			andIdCuenta = "      and b.cuentaBancaria.codigo is null";
		else
			andIdCuenta = "      and b.cuentaBancaria.codigo = :cuenta";
		Query query = em.createQuery (where+andIdBanco+andIdCuenta);
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		if(idBanco != 0L)
			query.setParameter ("banco", idBanco);
		if(idCuenta != 0L)
			query.setParameter ("cuenta", idCuenta);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoDesgloseDaoService#selectAuxDepositoDesgloseByIdusuarioPorCaja(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<AuxDepositoDesglose> selectAuxDepositoDesgloseByIdUsuarioPorCaja( Long idUsuario) throws Throwable {
		System.out.println("Ingresa al Metodo selectAuxDepositoDesgloseByIdusuarioPorCaja con id del usuario :" + idUsuario);
		Query query = em.createQuery(" select b " +
									 " from    AuxDepositoDesglose b" +
									 " where   b.usuarioPorCaja = :idUsuario" +
									 "		   and b.banco is null " +
									 "		   and b.cuentaBancaria is null ");
		query.setParameter("idUsuario", idUsuario);
		return query.getResultList();
	}

	public void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo eliminaPorUsuarioCaja con idUsuarioCaja: " + idUsuarioCaja);
		Query query = em.createQuery (" delete b " +
				  					  " from   AuxDepositoDesglose b " +
				  					  " where  b.usuarioPorCaja.codigo = :idUsuarioCaja");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		try {
			query.executeUpdate();
		} catch (Exception e) {
			throw new IncomeException("Error en el metodo eliminaPorUsuarioCaja(AuxDepositoDesglose): " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoDesgloseDaoService#selectByUsuarioCajaCuentaTipo(java.lang.Long, java.lang.Long)
	 */
	public Double selectEfectivoByUsuarioCajaCuenta( Long idUsuarioCaja, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectEfectivoByUsuarioCajaCuenta con id de usuario por caja: " + idUsuarioCaja + " id de cuenta: " + idCuenta);
		Query query = em.createQuery (" select b.valor " +
									  " from     AuxDepositoDesglose b " +
									  " where    b.usuarioPorCaja.codigo = :idUsuarioCaja "+
									  "          and b.cuentaBancaria.codigo = :cuenta "+
				  					  "          and b.tipo = :tipo ");
		query.setParameter ("idUsuarioCaja", idUsuarioCaja);
		query.setParameter ("cuenta", idCuenta);
		query.setParameter ("tipo", Long.valueOf(TipoDesgloseDeposito.EFECTIVO));
		Double saldo = 0.0;		
		if(!query.getResultList().isEmpty())
			saldo = Double.valueOf(query.getResultList().get(0).toString());		
		return saldo;
	}

	public void eliminaEfectivoDepositado(Long idUsuarioCaja, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo eliminaEfectivoDepositado con id de usuario por caja: " + idUsuarioCaja + " id de cuenta: " + idCuenta);
		Query query = em.createQuery (" delete b  " +
				  					  " from   AuxDepositoDesglose b " +
				  					  " where  b.usuarioPorCaja.codigo = :idUsuarioCaja"+
				  					  "        and b.tipo = :tipo " +
				  					  "        and b.cuentaBancaria.codigo = :cuenta");
		query.setParameter("idUsuarioCaja", idUsuarioCaja);
		query.setParameter("tipo", Long.valueOf(TipoDesgloseDeposito.EFECTIVO));
		query.setParameter("cuenta", idCuenta);
		try {
			query.executeUpdate();
		} catch (PersistenceException e) {
			throw new IncomeException("Error en el metodo eliminaEfectivoDepositado: " + e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.AuxDepositoDesgloseDaoService#actualizaChequesDesglose(java.lang.Long, java.lang.Long)
	 */
	public void actualizaChequesDesglose(Long idUsuarioCaja, Long idCuenta) throws Throwable {
		System.out.println("Ingresa al metodo actualizaChequesDesglose con id de usuario por caja: " + idUsuarioCaja + " id de cuenta: " + idCuenta);
		Query query = em.createQuery (" update AuxDepositoDesglose " +
				  					  " set		banco = null,"+
				  					  "         cuentaBancaria = null,"+
				  					  "         seleccionado = 0"+
				  					  " where   usuarioPorCaja.codigo = :idUsuarioCaja"+
									  "         and cuentaBancaria.codigo = :cuenta");
		query.setParameter("idUsuarioCaja", idUsuarioCaja);
		query.setParameter("cuenta", idCuenta);
		try {
			query.executeUpdate();
		} catch (PersistenceException e) {
			throw new IncomeException("Error en el metodo actualizaChequesDesglose: " + e.getCause());
		}
	}
	
}
