/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tsr.CuentaBancaria;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice CuentaBancaria.  
 */
@Local
public interface CuentaBancariaDaoService extends EntityDao<CuentaBancaria> {
	
	/**
	 * Recupera la cuenta contable de una cuenta bancaria
	 * @param idCuenta	: Id de cuenta bancaria
	 * @return			: Recupera la cuenta contable de una caja
	 * @throws Throwable: Excepcion
	 */
	 PlanCuenta recuperaCuentaContable(Long idCuenta) throws Throwable;
	
	/**
	 * Metodo que recupera el Banco y la Cuenta en base al Id de la Cuenta
	 * @param idCuentaBancaria	: Id de la Cuenta 
	 * @return					: Banco y Cuenta
	 * @throws Throwable		: Excepcions
	 */
	 CuentaBancaria recuperaBancoCuenta (Long idCuentaBancaria)throws Throwable;
	
	/**
	 * Recupera datos  de la cuenta bancaria
	 * @param idCuentaBancaria	: Id de la Cuenta Bancaria 
	 * @return					: Cuenta Bancaria
	 * @throws Throwable		: Excepcions 
	 */
	 List<CuentaBancaria> recuperaBancoCuentaById (Long idCuentaBancaria) throws Throwable;
	 
	 /**
	  * Recupera las cuentas de una empresa sin tomar en cuenta una cuenta en cierto estado
	 * @param empresa		: Id empresa
	 * @param numeroCuenta	: Numero de cuenta que no se desea incluir
	 * @param estado		: Estado de las cuentas bancarias
	 * @return				: Listado de elementos
	 * @throws Throwable	: Excepcion
	 */
	List<CuentaBancaria> selectByEmpresaSinCuenta(Long empresa, String numeroCuenta, Long estado) throws Throwable;

	/**
	 * Recupera el banco y el numero de cuenta en base al id de la cuenta bancaria
	 * @param idCuenta	: Id de la Cuenta Bancaria
	 * @return			: Id Cuenta
	 * @throws Throwable: Excepcions
	 */
	List<CuentaBancaria> selectBancoNumeroByIdCuenta(Long idCuenta)throws Throwable;
	
	/**
	 * Identifica si el banco puede o no conciliar con valores descuadrados
	 * @param idCuenta	: Id de la Cuenta
	 * @return			: Conciliacion Descuadre
	 * @throws Throwable: Excepcions
	 */
	List<CuentaBancaria> selectConciliacionByDescuadre(Long idCuenta)throws Throwable;
	
	/**
	 * Select de cuentas por banco y cuenta
	 * @param idEmpresa		: Id Empresa
	 * @param idBanco		: Id Banco
	 * @param idCuenta		: Id Cuenta Bancaria
	 * @return				: Listado de Cuentas
	 * @throws Throwable	: Excepcion
	 */
	public List<CuentaBancaria> selectSaldosByBancoCta(Long idEmpresa, Long idBanco, Long idCuenta) throws Throwable;
 }
