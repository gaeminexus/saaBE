/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.DetalleConciliacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice DetalleConciliacion.  
 */
@Local
public interface DetalleConciliacionDaoService extends EntityDao<DetalleConciliacion>{
	
	/**
	 * Recupera un Historico de la conciliacion
	 * @param conciliacion	: Conciliacion
	 * @return				: Historico de Conciliacion
	 * @throws Throwable	: Excepcion
	 */
	List<DetalleConciliacion> selectHistoricaConciliacion(Long conciliacion)throws Throwable;
	
	/**
	 * Recupera Codigo por la conciliacion
	 * @param idConciliacion: Id de la Conciliacion
	 * @return				: Codigo
	 * @throws Throwable	: Excepcions
	 */
	List<DetalleConciliacion> selectByIdConciliacion(Long idConciliacion)throws Throwable;
	
	/**
	 * Eliminar todos los registros filtrados por idConciliacion
	 * @param idConciliacion: Id de la conciliacion 
	 * @throws Throwable: Excepcions
	 */
	void deleteByIdConciliacion(Long idConciliacion) throws Throwable;

}
