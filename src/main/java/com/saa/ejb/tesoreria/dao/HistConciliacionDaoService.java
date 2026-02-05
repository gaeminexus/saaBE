/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.HistConciliacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice HistConciliacion.  
 */
@Local
public interface HistConciliacionDaoService extends EntityDao<HistConciliacion>{
	
	/**
	 * Recupera los registros por cuenta bancaria, periodo y id conciliacion
	 * @param idCuenta	: Id de la cuenta bancaria
	 * @param idPeriodo	: Id del periodo
	 * @param idConciliacionOrigen: Id de la conciliacion origen del respaldo
	 * @return			: Historico que pertenece a una cuenta en un periodo y de una conciliacion
	 * @throws Throwable: Excepcion
	 */
	HistConciliacion selectByCuentaPeriodoConciliacion(Long idCuenta, Long idPeriodo, 
			Long idConciliacionOrigen) throws Throwable;	

}
