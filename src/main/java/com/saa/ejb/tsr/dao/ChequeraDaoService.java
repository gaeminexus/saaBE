/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.Chequera;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice Chequera.
 */
@Local
public interface ChequeraDaoService extends EntityDao<Chequera> {

	/**
	 * Mayor número final entre las chequeras no anuladas de la cuenta.
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @return					: Mayor finaliza, o null si la cuenta no tiene chequeras
	 * @throws Throwable		: Excepcion
	 */
	Long selectMaxFinalizaByCuenta(Long idCuentaBancaria) throws Throwable;

	/**
	 * Indica si el rango [comienza, finaliza] se solapa con alguna chequera no
	 * anulada de la misma cuenta.
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @param comienza			: Número inicial del rango a validar
	 * @param finaliza			: Número final del rango a validar
	 * @return					: true si hay solape
	 * @throws Throwable		: Excepcion
	 */
	boolean existeSolape(Long idCuentaBancaria, Long comienza, Long finaliza) throws Throwable;

	/**
	 * Chequeras de una cuenta bancaria, ordenadas por su número inicial.
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @return					: Chequeras de la cuenta
	 * @throws Throwable		: Excepcion
	 */
	List<Chequera> selectByCuentaBancaria(Long idCuentaBancaria) throws Throwable;

}
