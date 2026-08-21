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
import com.saa.model.rhh.Nomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService Nomina. 
 */
@Local
public interface NominaDaoService  extends EntityDao<Nomina>  {
	

	/**
	 * Recupera las nominas de un periodo, ordenadas por empleado.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: Listado de nominas; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<Nomina> selectByPeriodo(Long idPeriodo) throws Throwable;

	/**
	 * Recupera la nomina de un empleado en un periodo.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @param idEmpleado	: Id del empleado
	 * @return				: La nomina, o null si el empleado no fue procesado
	 * @throws Throwable	: Excepcion
	 */
	Nomina selectByPeriodoYEmpleado(Long idPeriodo, Long idEmpleado) throws Throwable;
}
