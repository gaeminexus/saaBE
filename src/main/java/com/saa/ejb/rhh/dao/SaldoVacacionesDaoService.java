/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.SaldoVacaciones;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService SaldoVacaciones. 
 */
@Local
public interface SaldoVacacionesDaoService  extends EntityDao<SaldoVacaciones>  {
	

	/**
	 * Recupera los saldos de vacaciones con dias disponibles de un empleado, del mas
	 * antiguo al mas reciente, que es el orden de consumo FIFO.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @return				: Listado de saldos; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<SaldoVacaciones> selectDisponibles(Long idEmpleado) throws Throwable;

	/**
	 * Recupera el saldo de vacaciones de un empleado para un anio concreto. Se usa para
	 * que la acreditacion anual sea idempotente.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio del periodo de vacaciones
	 * @return				: El saldo, o null si aun no se acredito
	 * @throws Throwable	: Excepcion
	 */
	SaldoVacaciones selectByEmpleadoYAnio(Long idEmpleado, Integer anio) throws Throwable;
}
