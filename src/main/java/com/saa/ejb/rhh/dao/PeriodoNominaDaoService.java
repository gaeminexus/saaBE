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

	/**
	 * Recupera el ultimo periodo MENSUAL CERRADO de una empresa, el de mayor anio y mes. Es el
	 * corte natural de la acreditacion de vacaciones: los periodos de decimos, utilidades y
	 * liquidacion no marcan hasta donde llego la nomina del mes.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @return				: El periodo, o null si la empresa no ha cerrado ninguno
	 * @throws Throwable	: Excepcion
	 */
	PeriodoNomina selectUltimoMensualCerrado(Long idEmpresa) throws Throwable;


	/**
	 * Periodos MENSUALES de una empresa, sin los anulados, entre dos claves anio-mes (anio*100+mes,
	 * ambas inclusive). Es la base de la precondicion del decimo tercero anticipado: que meses de
	 * la ventana estan cerrados y cuales solo calculados. Ordenados por anio y mes.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param claveDesde	: Clave anio*100+mes inicial
	 * @param claveHasta	: Clave anio*100+mes final
	 * @return				: Los periodos; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	java.util.List<PeriodoNomina> selectMensualesEntre(Long idEmpresa, Integer claveDesde, Integer claveHasta)
			throws Throwable;
}
