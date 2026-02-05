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
import com.saa.model.tsr.MotivoPago;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice MotivoPago.  
 */
@Local
public interface MotivoPagoDaoService extends EntityDao<MotivoPago>{
	
	/**
	 * Recupera los Motivos por Id Pago
	 * @param idPago
	 * @return
	 * @throws Throwable
	 */
	List<MotivoPago> selectByIdPago(Long idPago) throws Throwable;
	
	

}
