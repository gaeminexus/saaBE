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
import com.saa.model.tsr.AuxDepositoCierre;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice AuxDepositoCierre.  
 */
@Local
public interface AuxDepositoCierreDaoService extends EntityDao<AuxDepositoCierre> {	

	/**
	 * Recupera el auxiliar de cierre segun el usuario caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @return				: Auxiliar de cierre recuperado
	 * @throws Throwable	: Excepcion
	 */
	List<AuxDepositoCierre> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Recupera CierreCaja para Actualizar los cierres
	 * @param idUsuario	: Id del Usuario
	 * @return			: cierre cajas
	 * @throws Throwable: Excepcions
	 */
	List<AuxDepositoCierre> selectCierreCajaByIdUsuario(Long idUsuario)throws Throwable;

	/**
	 * Elimina los registros temporales por usuario por caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @throws Throwable	: Excepcion
	 */
	void eliminaPorUsuarioCaja(Long idUsuarioCaja) throws Throwable;
}
