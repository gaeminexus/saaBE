package com.saa.ejb.tsr.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tsr.CuentaBancaria;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CuentaBancaria.
 *  Accede a los metodos DAO y procesa los datos para el cliente</p>
 */
@Local
public interface CuentaBancariaService extends EntityService<CuentaBancaria>{
	
	 /**
	  * Valida si una cuenta destino bancaria esta conciliada
	  * @param empresa	: Id de la empresa
	  * @param idCuenta	: Id de la cuenta bancaria
	  * @throws Throwable: Excepcion
	  */
	  void validaCuentaDestinoConciliada(Long empresa, Long idCuentaBancaria) throws Throwable;
	 
	 /**
	  * busca la cuenta contable en una transferencia, cuenta contable de cuenta bancaria destino
	  * @param idCuenta	: Id de la cuenta bancaria
	  * @return			: Entidad Plan de cuenta
	  * @throws Throwable: Excepcion
	  */
	  PlanCuenta buscarCuentaContableTranferencia(Long idCuenta) throws Throwable;
	 
	/**
	 * Obtener el estado de la cuenta origen
	 * @param idCuentaBancaria	: Id de la Cuenta Bancaria
	 * @return					: Banco, Cuenta, Estado
	 * @throws Throwable		: Excepcions
	 */
	 CuentaBancaria recuperaBancoCuentaById (Long idCuentaBancaria) throws Throwable;
	 
	/**
	 * Metodo para cambiar estado de pendiente a activa
	 * @param idCuentaBancaria 	: Id de la cuenta
	 * @param valorDeposito		: Valor a depositar
	 * @throws Throwable		: Excepcion
	 */
	void cambiaEstadoCuenta (Long idCuentaBancaria, Double valorDeposito)throws Throwable;
	
	/**
	 * Metodo para cambiar estado de pendiente a activa
	 * @param idCuentaBancaria 	: Id de la cuenta
	 * @param planCuenta		: Entidad plan cuenta
	 * @param valorDeposito		: Valor a depositar
	 * @throws Throwable		: Excepcion
	 */
	void cambiaEstadoCuenta (Long idCuentaBancaria, PlanCuenta planCuenta, Double valorDeposito)throws Throwable;
	
	/**
	  * Recupera las cuentas de una empresa sin tomar en cuenta una cuenta en cierto estado
	 * @param empresa		: Id empresa
	 * @param numeroCuenta	: Numero de cuenta que no se desea incluir
	 * @param estado		: Estado de las cuentas bancarias
	 * @param campos		: Campos que se van a recuperar
	 * @return				: Listado de elementos
	 * @throws Throwable	: Excepcion
	 */
	List<CuentaBancaria> selectByEmpresaSinCuenta(Long empresa, String numeroCuenta, Long estado, Object[] campos) throws Throwable;
	
	/**
	 * Obtiene el saldo de una cuenta bancaria a una fecha dad
	 * @param idCuenta	: Id de la cuenta bancaria
	 * @param fecha		: Fecha a la que se desea obtener el saldo
	 * @return			: Saldo de la cuenta bancaria
	 * @throws Throwable: Excepcion
	 */
	Double obtieneSaldoFecha(Long idCuenta, LocalDate fecha) throws Throwable;	
	
	/**
	 * Recupera los saldos de las cuentas de una empresa en un rango de fechas
	 * @param idEmpresa		: Id empresa
	 * @param fechaDesde	: Fecha inicial del rango 
	 * @param fechaHasta	: Fecha final del rango
	 * @param idBanco		: Id del banco 
	 * @param idCuenta		: Id de la cuenta
	 * @return				: Listado de las cuentas
	 * @throws Throwable	: Excepcion
	 */
	List<CuentaBancaria> selectSaldoCuentasByFecha(Long idEmpresa, Object[] campos,
		LocalDate fechaDesde, LocalDate fechaHasta, Long idBanco, Long idCuenta) throws Throwable;	
}
