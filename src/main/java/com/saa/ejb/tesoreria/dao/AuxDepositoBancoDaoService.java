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
import com.saa.model.tesoreria.AuxDepositoBanco;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice AuxDepositoBanco.  
 */
@Local
public interface AuxDepositoBancoDaoService extends EntityDao<AuxDepositoBanco> {
	
	/**
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
	 * @return				: Lista de auxiliar de deposito Banco
	 * @throws Throwable	: Excepcion
	 */
	List<AuxDepositoBanco> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	 
	/**
	 * Recupera el registro de auxiliarDepositoBanco a actualizar
	 * @param IdBanco		: Id del Banco
	 * @param codigoCuenta	: Codigo de la Cuenta Bancaria
	 * @return				: Registro
	 * @throws Throwable	: Excepcions
	 */
	List<AuxDepositoBanco> selectRegistroByBanco (Long idBanco, Long codigoCuenta) throws Throwable;
	
	/**
	 * Recupera AuxDepositoBanco por usuario
	 * @param idUsuario	: Id del Usuario
	 * @return			: AuxDeposito
	 * @throws Throwable: Excepcions
	 */
	List<AuxDepositoBanco> selectAuxDepositoBancoByIdUsuario (Long idUsuario)throws Throwable;
	
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
