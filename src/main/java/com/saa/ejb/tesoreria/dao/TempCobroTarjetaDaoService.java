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
import com.saa.model.tsr.TempCobroTarjeta;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice TempCobroTarjeta.  
 */
@Local
public interface TempCobroTarjetaDaoService extends EntityDao<TempCobroTarjeta>{
	
	/**
	 * Elimina cobros con tarjeta temporales
	 * @param idTempCobro	: Id del cobro
	 * @throws Throwable	: Excepcions
	 */
	void eliminaCobroTarjetaByIdCobro (Long idTempCobro) throws Throwable;
	
	/**
	 * Recupera los id de cobros temporales de un usuario 
	 * @param idTempCobro : Id de Temp cobro
	 * @return				: Lista de id de cobros temporales
	 * @throws Throwable	: Excepcion
	 */
	List<TempCobroTarjeta> selectByIdTempCobro(Long idTempCobro) throws Throwable;
	
}
