/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tesoreria.dao.SaldoBancoDaoService;
import com.saa.model.tsr.SaldoBanco;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion SaldoBancoDaoService.
 */
@Stateless
public class SaldoBancoDaoServiceImpl extends EntityDaoImpl<SaldoBanco> implements SaldoBancoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"cuentaBancaria",
							"periodo",
							"numeroMes",
							"numeroAnio",
							"saldoAnterior",
							"valorEgreso",
							"valorIngreso",
							"valorND",
							"valorNC",
							"saldoFinal",
							"valorTransferenciaD",
							"valorTransferenciaC"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.SaldoBancoDaoService#recuperaUltimoRegistro(java.lang.Long, java.util.LocalDate)
	 */
	public Long recuperaUltimoIdSaldo (Long idCuenta, Long mes, Long anio)throws Throwable {		
		System.out.println("Ingresa al metodo recuperaUltimoIdSaldo con Cuenta : " + idCuenta + " Fecha : " + mes+"/"+anio);
		Long codigo = 0L;
		Query query = em.createQuery(" select   max (b.codigo) " +
									 " from     SaldoBanco b " +
									 " where    b.cuentaBancaria.codigo = :idCuenta " +
									 "			and   b.periodo.ultimoDia < (select   d.ultimoDia" +
									 "										 from     Periodo d" +
									 "										 where	  d.empresa.codigo = b.periodo.empresa.codigo" +
									 "												  and   d.mes = :mes " +
									 " 								   	 			  and   d.anio = :anio)");
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("mes", mes);
		query.setParameter("anio", anio);		
		if(query.getResultList().get(0) != null){
			codigo = Long.valueOf(query.getResultList().get(0).toString());
		}
		System.out.println("codigo: "+codigo);
		return codigo;	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.SaldoBancoDaoService#selectByCuentaPeriodo(java.lang.Long, java.lang.Long)
	 */
	public SaldoBanco selectByCuentaPeriodo(Long idCuenta, Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al Metodo selectByCuentaPeriodo con id cuenta: " + idCuenta
				+ ", idPeriodo" + idPeriodo);
		Query query = em.createQuery(" select b " +
									 " from   SaldoBanco b " +
									 " where  b.cuentaBancaria.codigo = :idCuenta" +
									 "		  and   b.periodo.codigo = :idPeriodo");
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("idPeriodo", idPeriodo);
		return (SaldoBanco)query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.SaldoBancoDaoService#selectSaldoFinalByIdPeriodoAnioCuenta(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<SaldoBanco> selectSaldoFinalByIdPeriodoAnioCuenta( Long idPeriodo, Long anio, Long idCuentaBancaria) throws Throwable {
		System.out.println("Ingresa al Metodo selectSaldoFinalByIdPeriodoAnioCuenta con idPeriodo:" + idPeriodo + ", anio" + anio + ", idCuentaBancaria");
		Query query = em.createQuery(" select b " +
									 " from   SaldoBanco b " +
									 " where  b.numeroMes = :idPeriodo" +
									 "        and b.numeroAnio = :anio" +
									 "		  and b.cuentaBancaria = :idCuentaBancaria");
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("anio", anio);
		query.setParameter("cuentaBancaria", idCuentaBancaria);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.SaldoBancoDaoService#selectSaldoFinalByCuenta(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<SaldoBanco> selectSaldoFinalByCuenta(Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectSaldoFinalByCuenta con idCuenta: " + idCuenta);
		Query query = em.createQuery(" select b " +
									 " from   SaldoBanco b " +
									 " where  b.codigo = :idCuenta");
		query.setParameter("idCuenta", idCuenta);		
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.SaldoBancoDaoService#selectMaxCuentaMenorFecha(java.lang.Long, java.util.LocalDate)
	 */
	public SaldoBanco selectMaxCuentaMenorFecha(Long idCuenta, LocalDate fecha)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectMaxCuentaMenorFecha con idCuenta: " + idCuenta 
				 + ", fecha: " + fecha);
		SaldoBanco saldo = new SaldoBanco();
		Query query = em.createQuery(" select b " +
									 " from   SaldoBanco b" +
									 " where  b.codigo = (" +
									 "                    select   max (c.codigo) " +
									 "                    from     SaldoBanco c " +
									 "                    where    c.cuentaBancaria.codigo = :idCuenta " +
									 "			                   and   c.periodo.ultimoDia < :fecha" +
									 "					  )");
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("fecha", fecha);
		try {
			saldo = (SaldoBanco)query.getSingleResult();
		} catch (NoResultException e) {
			saldo = null;
		}
		return saldo;
	}
}
