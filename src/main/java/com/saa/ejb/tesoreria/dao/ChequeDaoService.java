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
import com.saa.model.tesoreria.Cheque;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice Cheque.  
 */
@Remote
public interface ChequeDaoService extends EntityDao<Cheque> {
	
	/**
	 * Recupera el Maximo Cheqhe se la chequera segun la cuenta
	 * @param cuenta		: Numero de la cuenta 
	 * @return
	 * @throws Throwable	: Excepcion
	 */
	List<Cheque> selectMaxCheque (Long cuenta)throws Throwable;
	 
	 /**
	  * Recupera el Maximo Cheqhe se la chequera segun la cuenta
	  * @param cuenta		: Id de la cuenta 
	  * @return				: Id de primer cheque activo de una cuenta 
	  * @throws Throwable	: Excepcion
	  */
	Long selectMinChequeActivo (Long idCuenta)throws Throwable;
	
}