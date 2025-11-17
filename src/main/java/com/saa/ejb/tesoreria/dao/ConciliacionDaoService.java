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
import com.saa.model.tesoreria.Conciliacion;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice Conciliacion.  
 */
/**
 * @author gs-prog05
 *
 */
@Remote
public interface ConciliacionDaoService extends EntityDao<Conciliacion>{
	
	/**
	 * Recupera registros para conciliar 
	 * @param idCuentaBancaria	: Id de la Cuenta Bancaria
	 * @param idPeriodo			: Id del Periodo
	 * @return					: 
	 * @throws Throwable		: Excepcions
	 */
	 Long selectValidacion (Long idCuentaBancaria, Long idPeriodo) throws Throwable;
	 
	/**
	 * Recupera conteo de los registros conciliados 
	 * @param idPeriodo	: Id del Periodo
	 * @param idCuenta	: Id de la Cuenta
	 * @return			: Conteo 
	 * @throws Throwable: Excepcions
	 */
	 int selectRegistrosByConciliacion(Long idPeriodo, Long idCuenta)throws Throwable;
	 
	/**
	 * Recupera el id de la Conciliacion
	 * @param idPeriodo	: Id del Periodo
	 * @param idCuenta	: Id de la Cuenta
	 * @return			: Id Conciliacion
	 * @throws Throwable: Excepcions
	 */
	List<Conciliacion> selectIdConciliacion(Long idPeriodo, Long idCuenta)throws Throwable;
	
	/**
	 * Recupera listado Conciliacion por el idConciliacion
	 * @param idConciliacion	: Id de la Conciliacion
	 * @return					: Listado de Conciliacion 
	 * @throws Throwable		: Excepcions
	 */
	List<Conciliacion> selectConciliacionByIdConciliacion(Long idConciliacion)throws Throwable;	
	
	/**
	 * Recupera los registros por id de periodo, id de cuenta bancaria y estado 
	 * @param idPeriodo	: Id del Periodo
	 * @param idCuenta	: Id de la Cuenta
	 * @param estado	: Estado
	 * @return			: Registro que cumple condicion 
	 * @throws Throwable: Excepcions
	 */
	Conciliacion selectByPeriodoCuentaEstado(Long idPeriodo, Long idCuenta, int estado) throws Throwable;
	
	/**
	 * Recupera los registros por id de periodo, id de cuenta bancaria y estado
	 * @param idCuenta	: Id de la Cuenta 
	 * @param idPeriodo	: Id del Periodo
	 * @return			: Registro que cumple condicion 
	 * @throws Throwable: Excepcions
	 */
	Conciliacion selectByCuentaPeriodo(Long idCuenta, Long idPeriodo) throws Throwable;
	
	/**
	 * Eliminar todos los registros filtrados por idConciliacion
	 * @param idConciliacion: Id de la conciliacion 
	 * @throws Throwable: Excepcions
	 */
	void deleteByIdConciliacion(Long idConciliacion) throws Throwable;

}