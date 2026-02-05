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
import com.saa.model.tsr.TempPago;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice TempPago.  
 */
@Local
public interface TempPagoDaoService extends EntityDao<TempPago>{
	
	/**
	 * Recupera los id de pagos temporales de un usuario
	 * @param idUsuario : Id de ususario
	 * @return			: Lista de id de pagos temporales
	 * @throws Throwable: Excepcion
	 */
	 List<Long> selectByUsuario(Long idUsuario) throws Throwable;

	 /**
	  * Elimina todos los pagos de un usuario
	  * @param idUsuario	: Id Usuario
	  * @throws Throwable	: Excepcion
	  */
	 void eliminaPagoByIdUsuario (Long idUsuario) throws Throwable;
}
