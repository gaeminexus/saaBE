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
import com.saa.model.tesoreria.TempMotivoPago;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice TempMotivoPago.  
 */
@Remote
public interface TempMotivoPagoDaoService extends EntityDao<TempMotivoPago>{

	/**
	 * Elimina los Motivos de Pago temporales 
	 * @param idTempPago	: Id del pago temporal
	 * @throws Throwable	: Excepcion
	 */
	void eliminaMotivoPagoByIdPago (Long idTempPago) throws Throwable;
	
	
	/**
	 * Recupera el detalle de un asiento
	 * @param idAsiento	: Id de asiento
	 * @return			: Listado con el detalle de asiento
	 * @throws Throwable: Excepcion
	 */
	List<TempMotivoPago> selectByIdTempPago(Long idTempPago) throws Throwable;
	
}