/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.dao;

import java.time.LocalDate;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.PeriodoNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService PeriodoNomina.
 */
@Local
public interface PeriodoNominaDaoService  extends EntityDao<PeriodoNomina>  {

	/**
	 * Recupera el periodo de nomina de una empresa que contiene una fecha, es decir
	 * fechaInicio &lt;= fecha &lt;= fechaFin.
	 *
	 * @param idEmpresa	: Id de la empresa
	 * @param fecha		: Fecha a ubicar
	 * @return			: El periodo, o null si ninguno la contiene
	 * @throws Throwable	: Excepcion
	 */
	PeriodoNomina selectByFechaEmpresa(Long idEmpresa, LocalDate fecha) throws Throwable;

}