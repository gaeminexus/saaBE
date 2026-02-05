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
import com.saa.model.tsr.AuxDepositoDesglose;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice AuxDepositoDesglose.  
 */
@Local
public interface AuxDepositoDesgloseDaoService extends EntityDao<AuxDepositoDesglose> {	
	
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
	 * Eliminar registro restante de efectivo
	 * @param idUsuarioCaja: Id de usuario caja
	 * @return				: auxiliares de desglose
	 * @throws Throwable	: Excepciones
	 */
	void eliminaEfectivoRestante(Long idUsuarioCaja) throws Throwable;
	 
	/**
	 * Eliminar registro depositado de efectivo
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @param idCuenta		: Id de cuenta bancaria
	 * @return				: auxiliares de desglose
	 * @throws Throwable	: Excepciones
	 */
	void eliminaEfectivoDepositado(Long idUsuarioCaja, Long idCuenta) throws Throwable;
	
	/**
	 * Recupera la suma de efectivo por usuario caja, banco y cuenta
	 * @param idUsuarioCaja: Id de usuario por caja
	 * @param idBanco		: Id de banco
	 * @param idCuenta		: Id de cuenta bancaria
	 * @return				: suma de efectivo
	 * @throws Throwable	: Excepcion
	 */
	Double selectSumaEfectivo(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable;
		
	/**
	 * Recupera la suma de cheques por usuario caja, banco y cuenta
	 * @param idUsuarioCaja: Id de usuario por caja
	 * @param idBanco		: Id de banco
	 * @param idCuenta		: Id de cuenta bancaria
	 * @return				: suma de efectivo
	 * @throws Throwable	: Excepcion
	 */
	Double selectSumaCheque(Long idUsuarioCaja, Long idBanco, Long idCuenta) throws Throwable;
	 
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
	 * Recupera auxiliares de desglose de un usuario caja, banco y cuenta
	 * @param idUsuarioCaja: Id de usuario caja
	 * @param idCuenta		: Id de cuenta bancaria 
	 * @return				: Saldo efectivo
	 * @throws Throwable	: Excepciones
	 */
	Double selectEfectivoByUsuarioCajaCuenta(Long idUsuarioCaja, Long idCuenta) throws Throwable;
	 
	/**
	 * Recupera los Registros con el Id Usuario para Actualizar
	 * @param idUsuario	: Id del Usuario
	 * @return			: Registros 
	 * @throws Throwable: Excepcions
	 */
	List<AuxDepositoDesglose> selectAuxDepositoDesgloseByIdUsuarioPorCaja (Long idUsuario)throws Throwable;
	
	/**
	 * Elimina los registros temporales por usuario por caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @throws Throwable	: Excepcion
	 */
	void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Actualiza los registros de cheques asignados al banco
	 * @param idUsuarioCaja: Id de usuario caja
	 * @param idCuenta		: Id de cuenta bancaria 
	 * @throws Throwable	: Excepciones
	 */
	void actualizaChequesDesglose(Long idUsuarioCaja, Long idCuenta) throws Throwable;
 }
