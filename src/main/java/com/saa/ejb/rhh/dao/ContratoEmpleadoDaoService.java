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
import com.saa.model.rhh.ContratoEmpleado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService Contrato Empleado. 
 */
@Local
public interface ContratoEmpleadoDaoService  extends EntityDao<ContratoEmpleado>  {
	

	/**
	 * Recupera los contratos que se solapan con un periodo: iniciados antes del fin del
	 * periodo y sin terminar, o terminados despues del inicio. Es la seleccion base del
	 * motor de nomina.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param desde			: Fecha de inicio del periodo
	 * @param hasta			: Fecha de fin del periodo
	 * @return				: Listado de contratos; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<ContratoEmpleado> selectActivosEnPeriodo(Long idEmpresa, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable;
}
