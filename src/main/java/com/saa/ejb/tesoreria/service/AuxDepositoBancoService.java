package com.saa.ejb.tesoreria.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.AuxDepositoBanco;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad AuxDepositoBanco.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface AuxDepositoBancoService extends EntityService<AuxDepositoBanco>{
	

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	AuxDepositoBanco selectById(Long id) throws Throwable;

	/**
	 * Recupera Id de Auxiliar de deposito Banco por usuario caja, banco y cuenta
	 * @param idUsuarioCaja	: Id de usuario por caja
	 * @param idBanco		: Id de banco
	 * @param idCuenta		: Id de cuenta bancaria
	 * @return				: Recupera Auxiliar de deposito Banco
	 * @throws Throwable	: Excepcion
	 */
	AuxDepositoBanco selectIdByUsuarioCajaBancoCuenta(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable;
	 
	/**
	 * Recupera Auxiliar de deposito Banco por usuario caja
	 * @param idUsuarioCaja	: Id de usuario por caja
	 * @return				: Recupera Lista de Auxiliar de deposito Banco
	 * @throws Throwable	: Excepcion
	 */
	List<AuxDepositoBanco> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	 
	/**
	 * Actualiza los saldos de un registro ingresado
	 * @param auxDepositoBanco	: Entidada AuxDepositoBanco
	 * @param efectivo			: Valor de efectivo  a depositar
	 * @param cheque			: Valor de cheque  a depositar
	 * @throws Throwable		: Excepcion
	 */
	void actualizaSaldosCuenta(AuxDepositoBanco auxDepositoBanco, Double efectivo, Double cheque) throws Throwable;
	 
	/**
	 * Elimina los registros temporales por usuario por caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @throws Throwable	: Excepcion
	 */
	void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Elimina los registros temporales por usuario por caja y cuenta
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @param idCuenta		: Id de cuenta bancaria
	 * @return				: auxiliares de desglose
	 * @throws Throwable	: Excepciones
	 */
	void eliminaPorUsuarioCajaCuenta(Long idUsuarioCaja, Long idCuenta) throws Throwable;
}
