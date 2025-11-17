package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.Asiento;
import com.saa.model.tesoreria.DebitoCredito;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DebitoCredito.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface DebitoCreditoService extends EntityService<DebitoCredito>{
 	 
	/**
	 * Proceso para generar debito bancario
	 * @param idEmpresa		: Id de empresa
	 * @param idUsuario		: Id de usuario
	 * @param idCuenta		: Id de cuenta bancaria
	 * @param nombreUsuario	: Nombre de usuario
	 * @param descripcion	: Descripcion
	 * @return				: Id Asiento y mensaje de salida
	 * @throws Throwable	: Excepcion	
	 */
	String[] generarDebitoBancario(Long idEmpresa, Long idUsuario, Long idCuenta, String nombreUsuario, String descripcion)throws Throwable;
	
	/**
	 * Proceso para generar credito bancario
	 * @param idEmpresa		: Id de empresa
	 * @param idUsuario		: Id de usuario
	 * @param idCuenta		: Id de cuenta bancaria
	 * @param nombreUsuario	: Nombre de usuario
	 * @param descripcion	: Descripcion
	 * @return				: Id Asiento y mensaje de salida
	 * @throws Throwable	: Excepcion	
	 */
	String[] generarCreditoBancario(Long idEmpresa, Long idUsuario, Long idCuenta, String nombreUsuario, String descripcion)throws Throwable;
	
	/**
	 * Almacena los datos de la cabecera del debito o credito
	 * @param idUsuario			: Id de usuario
	 * @param idCuenta			: Id de cuenta bancaria
	 * @param nombreUsuario		: Nombre de usuario
	 * @param descripcion		: Descripcion
	 * @param tipoDebitoCredito	: Tipo de movimiento (Debito = 1 o credito = 2)
	 * @return					: Debito-credito generado
	 * @throws Throwable		: Excepcion
	 */
	DebitoCredito insertarCabeceraDebitoCredito(Long idUsuario, Long idCuenta, String nombreUsuario, String descripcion, int tipoDebitoCredito)throws Throwable;
	
	/**
	 * Genera el asiento contable de credito bancario
	 * @param empresa			: Id de la Empresa
	 * @param usuario			: Nombre Usuario 
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @param debitoCredito		: Debito-Credito
	 * @param valorDeposito		: Valor a Depositar
	 * @return					: Id asiento, numero asiento, id de movimiento de conciliacion
	 * @throws Throwable		: Excepcion
	 */
	Long[] generaAsientoCredito(Long empresa, String usuario, Long idCuentaBancaria, DebitoCredito debitoCredito, Double valorDeposito) throws Throwable ;
	
	/**
	 * Genera el asiento contable de debito bancario
	 * @param empresa			: Id de la Empresa
	 * @param usuario			: Nombre Usuario 
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @param debitoCredito		: Debito-Credito
	 * @param valorDeposito		: Valor a Depositar
	 * @return					: Id asiento, numero asiento, id de movimiento de conciliacion
	 * @throws Throwable		: Excepcion
	 */
	Long[] generaAsientoDebito(Long empresa, String usuario, Long idCuentaBancaria, DebitoCredito debitoCredito, Double valorDeposito) throws Throwable ;
	
	/**
	 * Inserta detalle de asiento en el haber -motivos de creditos bancarios
	 * @param asiento		: Asiento
	 * @param debitoCredito	: Debito-Credito
	 * @throws Throwable	: Excepcion
	 */
	void insertaAsientoHaberCredito(Asiento asiento, DebitoCredito debitoCredito)throws Throwable;
	
	/**
	 * inserta detalle de asiento en el debe -motivos de debitos bancarios
	 * @param asiento		: Asiento
	 * @param debitoCredito	: Debito-Credito
	 * @throws Throwable	: Excepcion
	 */
	void insertaAsientoDebeDebito(Asiento asiento, DebitoCredito debitoCredito)throws Throwable;
	
	/**
	 * Actualiza el asiento y la conciliacion de un debito o credito
	 * @param debitoCredito	: Entidad a actualizar
	 * @param idAsiento		: Id de asiento
	 * @param numeroAsiento	: Numero de asiento
	 * @param idMovimiento	: Id de movimiento bancario
	 * @throws Throwable	: Excepcion
	 */
	void actualizaAsientoConciliacion(DebitoCredito debitoCredito, Long idAsiento, Long numeroAsiento, Long idMovimiento)throws Throwable;
}