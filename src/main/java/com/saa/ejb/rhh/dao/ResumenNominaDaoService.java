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
import com.saa.model.rhh.ResumenNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ResumenNomina. 
 */
@Local
public interface ResumenNominaDaoService  extends EntityDao<ResumenNomina>  {
	

	/**
	 * Suma los dias de ausencia no remunerada de un empleado en un rango. Son los que
	 * se restan de los dias trabajados del periodo.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param desde			: Fecha de inicio del rango
	 * @param hasta			: Fecha de fin del rango
	 * @param tipos			: Codigos alternos de los tipos de ausencia que descuentan
	 * @return				: Numero de dias; cero si no hay
	 * @throws Throwable	: Excepcion
	 */
	Double contarDiasAusenciaNoRemunerada(Long idEmpleado, java.time.LocalDate desde,
			java.time.LocalDate hasta, List<Long> tipos) throws Throwable;
}
