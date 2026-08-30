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
	 * Saldo de una cuenta bancaria a una fecha, armado desde {@code TSR.MVCB}
	 * (último cierre de {@code SaldoBanco} más los movimientos del rango).
	 *
	 * <p><b>NO es el saldo disponible de la cuenta — renombrado el 2026-08-27 desde
	 * {@code obtieneSaldoFecha}, que invitaba a usarlo para eso.</b> Solo lo alimentan
	 * ciertos procesos (pagos, cheques, caja chica); un asiento contable directo sobre
	 * la cuenta del banco no genera {@code MovimientoBanco}. Medido contra la
	 * contabilidad (ver docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md
	 * §7bis): BANCO PACIFICO tenía $2.016.302,36 en contabilidad y $22.802,11 aquí
	 * (1,1%); BANCO INTERNACIONAL, $2.714.031,22 contra $125.452,02 (4,6%).</p>
	 *
	 * <p>Para el saldo real de una cuenta (disponibilidad, ecuación de conciliación)
	 * usar {@code PlanCuentaService.saldoCuentaFechaEmpresa(idEmpresa,
	 * cuenta.getPlanCuenta().getCodigo(), fecha)}, que lee la contabilidad. Este método
	 * queda para lo que sí mide bien: el propio circuito de {@code MovimientoBanco}
	 * (conciliación banco↔libros, no banco↔"cuánto hay").</p>
	 *
	 * @param idCuenta	: Id de la cuenta bancaria
	 * @param fecha		: Fecha a la que se desea el saldo
	 * @return			: Saldo según TSR.MVCB, no el saldo contable
	 * @throws Throwable: Excepcion
	 */
	Double saldoSegunMovimientosBanco(Long idCuenta, LocalDate fecha) throws Throwable;
	
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
