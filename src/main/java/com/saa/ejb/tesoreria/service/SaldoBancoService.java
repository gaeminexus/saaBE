package com.saa.ejb.tesoreria.service;

import java.util.Date;
import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.Periodo;
import com.saa.model.tesoreria.CuentaBancaria;
import com.saa.model.tesoreria.SaldoBanco;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad SaldoBanco.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface SaldoBancoService extends EntityService<SaldoBanco>{
	 
	/**
	 * Obtiene el saldo de una cuenta bancaria a una fecha dada
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @param fecha				: Fecha inicio
	 * @return					: Saldo de la cuenta bancaria a la fecha
	 * @throws Throwable		: Excepcion
	 */
	Double saldoCuentaFecha(Long idCuentaBancaria, Date fecha)throws Throwable;
	
	/**
	 * Recupera por id de cuenta bancaria y de periodo
	 * @param idCuenta	: Id de la Cuenta bancaria
	 * @param idPeriodo	: Id del Periodo
	 * @return			: Registro que cumple la condicion
	 * @throws Throwable: Excepcions
	 */
	SaldoBanco selectByCuentaPeriodo(Long idCuenta, Long idPeriodo)throws Throwable;
	
	/**
	 * Recupera el maximo saldo de una cuenta menor a una fecha
	 * @param idCuenta	: Id de la cuenta bancaria
	 * @param fecha		: Fecha 
	 * @return			: Maximo saldo bancario
	 * @throws Throwable: Excepcion
	 */
	SaldoBanco selectMaxCuentaMenorFecha(Long idCuenta, Date fecha) throws Throwable;
	
	/**
	 * Inserta saldos de todas las cuentas bancarias con movimientos
	 * @param idPeriodo	: Id del Periodo
	 * @throws Throwable: Excepcion
	 */
	void insertaSaldoCuentaBancaria(Long idPeriodo)throws Throwable;
	
	/**
	 * Inserta los saldos por cuenta bancaria y periodo definidos
	 * @param cuentaBancaria: CuentaBancaria
	 * @param periodo		: Periodo
	 * @param saldoAnterior	: Saldo anterior
	 * @param saldos		: Saldos por movimientos bancarios
	 * @param saldoFinal	: Saldo final
	 * @throws Throwable	: Excepcion
	 */
	void saveSaldoPorCuentaPeriodo(CuentaBancaria cuentaBancaria, Periodo periodo, Double saldoAnterior, Double[] saldos, Double saldoFinal) throws Throwable;
}