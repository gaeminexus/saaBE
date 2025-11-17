/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.dao;

import java.util.Date;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tesoreria.SaldoBanco;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice SaldoBanco.  
 */
@Remote
public interface SaldoBancoDaoService extends EntityDao<SaldoBanco> {

	/**
	 * Recupera el ultimo id del Saldo creado por la mayorizacion a una Fecha
	 * @param idCuenta	: Id de la Cuenta
	 * @param mes		: Mes para la busqueda
	 * @param anio		: Año para la busqueda
	 * @return			: Id de saldo
	 * @throws Throwable: Excepcion
	 */
	Long recuperaUltimoIdSaldo (Long idCuenta, Long mes, Long anio) throws Throwable;
		
	/**
	 * Recupera por id de cuenta bancaria y de periodo
	 * @param idCuenta	: Id de la Cuenta bancaria
	 * @param idPeriodo	: Id del Periodo
	 * @return			: Registro que cumple la condicion
	 * @throws Throwable: Excepcions
	 */
	SaldoBanco selectByCuentaPeriodo(Long idCuenta, Long idPeriodo)throws Throwable;
	
	/**
	 * Recupera el Saldo Final por Periodo Anio y Id de la cuenta bancaria de ratif. dep.
	 * @param idPeriodo			: Id del Periodo
	 * @param anio				: Anio
	 * @param idCuentaBancaria	: Id de la Cuenta Bancaria
	 * @return					: SaldoFinal
	 * @throws Throwable		: Excepcions
	 */
	List<SaldoBanco> selectSaldoFinalByIdPeriodoAnioCuenta(Long idPeriodo, Long anio, Long idCuentaBancaria)throws Throwable;
	
	//Obtiene el saldo final de una cuenta bancaria dado el slbccdgo
	/**
	 * Obtiene el saldo final de una cuneta dado el codigo
	 * @param idCuenta	: Id de la Cuenta de Ratificacion
	 * @return			: Saldo Final
	 * @throws Throwable: Excepcions
	 */
	List<SaldoBanco> selectSaldoFinalByCuenta(Long idCuenta)throws Throwable;
	
	/**
	 * Recupera el maximo saldo de una cuenta menor a una fecha
	 * @param idCuenta	: Id de la cuenta bancaria
	 * @param fecha		: Fecha 
	 * @return			: Maximo saldo bancario
	 * @throws Throwable: Excepcion
	 */
	SaldoBanco selectMaxCuentaMenorFecha(Long idCuenta, Date fecha) throws Throwable;
	
}