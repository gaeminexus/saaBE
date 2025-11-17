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
import com.saa.model.tesoreria.Pago;
import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice Pago.  
 */
@Remote
public interface PagoDaoService extends EntityDao<Pago>{
	
	/**
	 * Metodo para recuperar el pago por el Id del Temporal
	 * @param idTempPago	:Id del temporal del pago
	 * @return				:Pago
	 * @throws Throwable	:Excepcion
	 */
	Pago recuperaIdPago(Long idTempPago) throws Throwable;
	
	/**
	 * Metodo para recuperar el pago por el Id del Cheque
	 * @param idAsiento		:Id del Cheque
	 * @return				:Pago
	 * @throws Throwable	:Excepcion
	 */
	List<Pago> recuperaIdCheque(Long idCheque) throws Throwable;
	
	/**
	 * Actualiza idCheque y estado por idCheque
	 * @param idCheque
	 * @param estado
	 * @throws Throwable
	 */
	void updateEstadoIdChequeByIdCheque(Long idCheque, int estado) throws Throwable;
	
}