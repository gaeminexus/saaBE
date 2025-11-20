package com.saa.ejb.tesoreria.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.AuxDepositoDesglose;
import com.saa.model.tesoreria.Cobro;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad AuxDepositoDesglose.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface AuxDepositoDesgloseService extends EntityService<AuxDepositoDesglose>{	
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	AuxDepositoDesglose selectById(Long id) throws Throwable;
	 
	/**
	 * Almacena auxiliar deposito desglose cheques
	 * @param idUsuarioCaja		: Id usuario caja
	 * @param cobro				: Entidad cobro
	 * @throws Throwable			: Excepcion
	 */
	void insertarAuxDesgloseCheque(Long idUsuarioCaja, Cobro cobro) throws Throwable;
	 
	/**
	 * Almacena auxiliar deposito desglose efectivo
	 * @param idUsuarioCaja: Id usuario caja
	 * @param valor		: Valor en efectivo		
	 * @throws Throwable	: Excepcion
	 */
	void insertarAuxDesgloseEfectivo(Long idUsuarioCaja, Double valor) throws Throwable;
	 
	/**
	 * Almacena el registro de efectivo generado
	 * @param idUsuarioCaja: Id Usuario por caja
	 * @param idBanco		: Id de banco
	 * @param idCuenta		: Id de la cuenta bancaria
	 * @param valor		: Valor en efectivo a depositar
	 * @throws Throwable	: Excepcion
	 */
	void insertarRegistroEfectivo(Long idUsuarioCaja, Long idBanco, Long idCuenta, Double valor) throws Throwable;
	 
	/**
	 * Almacena el registro de efectivo generado
	 * @param auxDepositoDesglose	: Entidad auxiliar desglose deposito
	 * @param valor				: Valor en efectivo a depositar
	 * @throws Throwable			: Excepcion
	 */
	void insertarRegistroEfectivo(AuxDepositoDesglose auxDepositoDesglose, Double valor) throws Throwable;

	/**
	 * Actualiza registro de desglose
	 * @param auxDepositoDesglose	: Entidad Auxiliar deposito desglose
	 * @param idBanco				: Id de banco
	 * @param idCuenta				: Id de la cuenta bancaria
	 * @throws Throwable			: Excepcion
	 */
	void actualizaRegistroDesglose(AuxDepositoDesglose auxDepositoDesglose, Long idBanco, Long idCuenta) throws Throwable;
	
	/**
	 * actualiza el registro de efectivo
	 * @param idUsuarioCaja: Id Usuario por caja
	 * @param idBanco		: Id de banco
	 * @param idCuenta		: Id de la cuenta bancaria
	 * @param valor		: Valor en efectivo a depositar
	 * @throws Throwable	: Excepcion
	 */
	void actualizaEfectivo(Long idUsuarioCaja, Long idBanco, Long idCuenta, Double valor) throws Throwable;
	 
	/**
	 * Eliminar registro restante de efectivo
	 * @param idUsuarioCaja: Id de usuario caja
	 * @return				: auxiliares de desglose
	 * @throws Throwable	: Excepciones
	 */
	void eliminaEfectivoRestante(Long idUsuarioCaja) throws Throwable;
	 
	/**
	 * Recupera total de efectivo y cheque enviado a un banco
	 * @param idUsuarioCaja: Id de usuario por caja
	 * @param idBanco		: Id de banco
	 * @param idCuenta		: Id de cuenta bancaria
	 * @return				: Arreglo con suma de efectivo y cheques
	 * @throws Throwable	: Excepcion
	 */
	Double[] recuperaSumasEfectivoChequeDeBanco(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable;
	 
	/**
	 * Almacena todos los registros de cierre en el deposito
	 * @param idUsuarioCaja: Id Usuario por caja
	 * @param idBanco		: Id de banco
	 * @param idCuenta		: Id de la cuenta bancaria
	 * @throws Throwable	: Excepcion
	 */
	void registrarTodos(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable;
	 
	/**
	 * Recupera auxiliares de desglose de un usuario caja, banco y cuenta
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @param idBanco		: Id de banco
	 * @param idCuenta		: Id de cuenta bancaria 
	 * @return				: Lista de auxiliares de desglose
	 * @throws Throwable	: Excepciones
	 */
	List<AuxDepositoDesglose> selectByUsuarioCajaBancoCuenta(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable;
	
	/**
	 * Elimina los registros temporales por usuario por caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @throws Throwable	: Excepcion
	 */
	void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Recupera auxiliares de desglose de uns usuario caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @return				: Lista de auxiliares de desglose
	 * @throws Throwable	: Excepciones
	 */
	List<AuxDepositoDesglose> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Recupera el valor efectivo por usuario por caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @return				: auxiliares de desglose
	 * @throws Throwable	: Excepciones
	 */
	AuxDepositoDesglose selectEfectivoByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Metodo para eliminar el auxiliar de deposito banco
	 * @param idUsuarioCaja : Id Usuario caja
	 * @param idCuenta		: Id de cuenta bancaria
	 * @return				: Saldo efectivo 
	 * @throws Throwable	: Excepcion
	 */
	Double eliminarAuxDepositoBanco(Long idUsuarioCaja, Long idCuenta)throws Throwable;
	
	/**
	 * Actualiza saldo de efectivo a repartir
	 * @param auxDepositoDesglose 	: Entidad Auxiliar deposito desglose
	 * @param idUsuarioCaja			: Id de usuario caja
	 * @param valor					: valor
	 * @throws Throwable			: Excepcion
	 */
	void actualizaEfectivoARepartir(AuxDepositoDesglose auxDepositoDesglose, Long idUsuarioCaja, Double valor)throws Throwable;
	
	/**
	 * Inserta saldo de efectivo a repartir
	 * @param auxDepositoDesglose 	: Entidad Auxiliar deposito desglose
	 * @param idUsuarioCaja			: Id de usuario caja
	 * @param valor					: valor
	 * @throws Throwable			: Excepcion
	 */
	void insertarEfectivoARepartir(Long idUsuarioCaja, Double valor)throws Throwable;
}
