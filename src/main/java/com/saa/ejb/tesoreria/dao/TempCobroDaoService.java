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
import com.saa.model.tsr.TempCobro;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice TempCobro.  
 */
@Local
public interface TempCobroDaoService extends EntityDao<TempCobro>{
	
	/**
	 * Recupera el id de TempCobro
	 * @param idUsuarioCaja	: Id Usuario por Caja
	 * @return				: Id de temporal de cobros
	 * @throws Throwable	: Excepcion
	 */
	 Long selectIdByUsuarioCaja (Long idUsuarioCaja)throws Throwable;
	
	/**
	 * Elimina Todos los Cobros de Usuario por Caja
	 * @param idUsuarioCaja	: Id Usuario por Caja
	 * @throws Throwable	: Excepcions
	 */
	 void eliminaCobroByIdUsuarioCaja (Long idUsuarioCaja) throws Throwable;

	/**
	 * Recupera los id de cobros temporales de un usuario de caja
	 * @param idUsuarioCaja : Id de ususario caja
	 * @return				: Lista de id de cobros temporales
	 * @throws Throwable	: Excepcion
	 */
	 List<Long> selectByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
}
