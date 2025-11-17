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
import com.saa.model.tesoreria.TempCobroCheque;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice TempCobroCheque.  
 */
@Remote
public interface TempCobroChequeDaoService extends EntityDao<TempCobroCheque>{

	/**
	 * Elimina cobros con cheques temporales 
	 * @param idTempCobro	: Id del cobro
	 * @throws Throwable	: Excepcions
	 */
	void eliminaCobroChequeByIdCobro (Long idTempCobro) throws Throwable;
	
	/**
	 * Recupera los id de cobros temporales de un usuario de caja
	 * @param idTempCobro : Id de Temp cobro
	 * @return				: Lista de id de cobros temporales
	 * @throws Throwable	: Excepcion
	 */
	 List<TempCobroCheque> selectByIdTempCobro(Long idTempCobro) throws Throwable;
	
	

}