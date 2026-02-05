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
import com.saa.model.tsr.Cobro;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice Cobro.  
 */
@Local
public interface CobroDaoService extends EntityDao<Cobro>{
	
	/**
	 * Recupera al id del cobro ingresado
	 * @param cobro		: Entidad cobro
	 * @return			: Id del cobro
	 * @throws Throwable: Excepcion
	 */
	Long recuperaIdCobro(Cobro cobro) throws Throwable; 
	
	/**
	 * Recupera los cobro de un cierre de caja
	 * @param idCierreCaja	: Id de cierre de caja
	 * @return				: Lista de cobro
	 * @throws Throwable	: Excepcion
	 */
	List<Cobro> selectByCierreCaja(Long idCierreCaja) throws Throwable;
	
	/**
	 * Metodo que valida los cobros pendientes
	 * @param idUsuario	: Id del Usuario 
	 * @return			: Listado Cobro
	 * @throws Throwable: Excepcions
	 */
	List<Cobro> selectCobrosPendientes (Long idUsuario) throws Throwable;
	
	/**
	 * Recupera los cobros para cierre de caja
	 * @param idUsuarioCaja	: Id de usuario por caja
	 * @return				: Elementos recuperados
	 * @throws Throwable	: Excepcion
	 */
	@SuppressWarnings({ "rawtypes" })
	List selectVistaByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Recupera los cobros para cierre de caja
	 * @param idUsuarioCaja	: Id de usuario por caja
	 * @return				: Elementos recuperados
	 * @throws Throwable	: Excepcion
	 */
	List<Cobro> selectCobroByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Recupera Cobros asociados a un Cierre
	 * @param cierreCaja	: Cierre de Caja
	 * @return				: Listado Cobro
	 * @throws Throwable	: Excepcions
	 */
	List<Cobro> selectCobroByCierreCaja (Long cierreCaja)throws Throwable;
	
}
