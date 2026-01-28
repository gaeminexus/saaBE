/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.basico.ejb;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         <p>
 *         Servicio para la administracion de procesos relacionados con cuentas.
 *         </p>
 */
@Local
public interface FechaService {

	/**
	 * Suma o resta dias a una fecha dada
	 * 
	 * @param fecha       : Fecha de la que se desea sumar o restar dias
	 * @param numeroDias: Numero de dias a sumar o restar a la fecha. En caso de
	 *                    resta enviar numero negativo
	 * @return : Fecha sumada o restada los dias indicados
	 * @throws Throwable: Excepcion
	 */
	Date sumaRestaDias(Date fecha, int numeroDias) throws Throwable;

	/**
	 * Suma o resta dias a una fecha dada usando LocalDate
	 * 
	 * @param fecha       : Fecha de la que se desea sumar o restar dias
	 * @param numeroDias: Numero de dias a sumar o restar a la fecha. En caso de
	 *                    resta enviar numero negativo
	 * @return : Fecha sumada o restada los dias indicados
	 * @throws Throwable: Excepcion
	 */
	LocalDate sumaRestaDiasLocal(LocalDate fecha, int numeroDias) throws Throwable;

	/**
	 * Recupera el ultimo d�a del mes de un per�odo con el mes y anio
	 * 
	 * @param mes  :Mes
	 * @param anio : Anio
	 * @return : Ultimo dia del mes
	 * @throws Throwable: Excepcion
	 */
	Date ultimoDiaMesAnio(Long mes, Long anio) throws Throwable;

	/**
	 * Recupera el primer d�a del mes de un per�odo con el mes y anio
	 * 
	 * @param mes  :Mes
	 * @param anio : Anio
	 * @return : Primer dia del mes
	 * @throws Throwable: Excepcion
	 */
	Date primerDiaMesAnio(Long mes, Long anio) throws Throwable;
	
	/**
	 * Recupera el ultimo d�a del mes de un per�odo con el mes y anio
	 * 
	 * @param mes  :Mes
	 * @param anio : Anio
	 * @return : Ultimo dia del mes
	 * @throws Throwable: Excepcion
	 */
	LocalDate ultimoDiaMesAnioLocal(Long mes, Long anio) throws Throwable;

	/**
	 * Recupera el primer d�a del mes de un per�odo con el mes y anio
	 * 
	 * @param mes  :Mes
	 * @param anio : Anio
	 * @return : Primer dia del mes
	 * @throws Throwable: Excepcion
	 */
	LocalDate primerDiaMesAnioLocal(Long mes, Long anio) throws Throwable;

	/**
	 * Recupera el primer d�a de la semana de la fecha dada
	 * 
	 * @param fecha : Fecha de la que se desea obtener el dato
	 * @return : Primer dia de la semana
	 * @throws Throwable: Excepcion
	 */
	Date primerDiaSemana(Date fecha) throws Throwable;

	/**
	 * Recupera el primer d�a de la semana de la fecha dada usando LocalDate
	 * 
	 * @param fecha : Fecha de la que se desea obtener el dato
	 * @return : Primer dia de la semana
	 * @throws Throwable: Excepcion
	 */
	LocalDate primerDiaSemanaLocal(LocalDate fecha) throws Throwable;

	/**
	 * Recupera el primer d�a del mes de una fecha dada
	 * 
	 * @param fecha : Fecha de la que se desea obtener el dato
	 * @return : Primer dia del mes
	 * @throws Throwable: Excepcion
	 */
	Date primerDiaMes(Date fecha) throws Throwable;

	/**
	 * Recupera el primer d�a del mes de una fecha dada usando LocalDate
	 * 
	 * @param fecha : Fecha de la que se desea obtener el dato
	 * @return : Primer dia del mes
	 * @throws Throwable: Excepcion
	 */
	LocalDate primerDiaMesLocal(LocalDate fecha) throws Throwable;

	/**
	 * Recupera el primer d�a del anio de una fecha dada
	 * 
	 * @param fecha : Fecha de la que se desea obtener el dato
	 * @return : Primer dia del anio
	 * @throws Throwable: Excepcion
	 */
	Date primerDiaAnio(Date fecha) throws Throwable;

	/**
	 * Recupera el primer d�a del anio de una fecha dada usando LocalDate
	 * 
	 * @param fecha : Fecha de la que se desea obtener el dato
	 * @return : Primer dia del anio
	 * @throws Throwable: Excepcion
	 */
	LocalDate primerDiaAnioLocal(LocalDate fecha) throws Throwable;

	/**
	 * Verifica si es que un rango de fechas se traslapa en otros registros.
	 * 
	 * @param fechas      : Listado con fechas a verificar
	 * @param fechaDesde: Fecha desde
	 * @param fechaHasta: Fecha Hasta
	 * @return : True = existe traslape, False = no existe traslape.
	 * @throws Throwable: Excepcion.
	 */
	@SuppressWarnings("rawtypes")
	boolean verificaTraslape(List fechas, Date fechaDesde, Date fechaHasta) throws Throwable;

	/**
	 * Verifica si es que un rango de fechas se traslapa en otros registros.
	 * 
	 * @param fechas      : Listado con fechas a verificar
	 * @param fechaDesde: Fecha desde
	 * @param fechaHasta: Fecha Hasta
	 * @return : True = existe traslape, False = no existe traslape.
	 * @throws Throwable: Excepcion.
	 */
	@SuppressWarnings("rawtypes")
	boolean verificaTraslapeActualiza(List fechas, Date fechaDesde, Date fechaHasta) throws Throwable;

	/**
	 * Suma o resta meses a una fecha dada
	 * 
	 * @param fecha        : Fecha de la que se desea sumar o restar meses
	 * @param numeroMeses: Numero de meses a sumar o restar a la fecha. En caso de
	 *                     resta enviar numero negativo
	 * @return : Fecha sumada o restada los meses indicados
	 * @throws Throwable: Excepcion
	 */
	Date sumaRestaMeses(Date fecha, int numeroMeses) throws Throwable;

	/**
	 * Suma o resta meses a una fecha dada usando LocalDate
	 * 
	 * @param fecha        : Fecha de la que se desea sumar o restar meses
	 * @param numeroMeses: Numero de meses a sumar o restar a la fecha. En caso de
	 *                     resta enviar numero negativo
	 * @return : Fecha sumada o restada los meses indicados
	 * @throws Throwable: Excepcion
	 */
	LocalDate sumaRestaMesesLocal(LocalDate fecha, int numeroMeses) throws Throwable;

	/**
	 * Calcula la edad en base a la fecha de nacimiento
	 * 
	 * @param fechaNacimiento : Fecha de nacimiento
	 * @return : Texto que indica la edad
	 * @throws Throwable : Excepcion
	 */
	String calculoEdad(Date fechaNacimiento) throws Throwable;

	/**
	 * Calcula la edad en base a la fecha de nacimiento usando LocalDate
	 * 
	 * @param fechaNacimiento : Fecha de nacimiento
	 * @return : Texto que indica la edad
	 * @throws Throwable : Excepcion
	 */
	String calculoEdadLocal(LocalDate fechaNacimiento) throws Throwable;

	/**
	 * Calcula la diferencia en meses entre dos fechas,
	 * 
	 * @param fecha1 : Fecha 1
	 * @param fecha2 : Fecha 2
	 * @return : Resultado meses
	 * @throws Throwable: Excepcins
	 */
	Long diferenciaMeses(Date fecha1, Date fecha2) throws Throwable;

	/**
	 * Calcula la diferencia en meses entre dos fechas usando LocalDate
	 * 
	 * @param fecha1 : Fecha 1
	 * @param fecha2 : Fecha 2
	 * @return : Resultado meses
	 * @throws Throwable: Excepcins
	 */
	Long diferenciaMesesLocal(LocalDate fecha1, LocalDate fecha2) throws Throwable;

	/**
	 * Calcula la diferencia en minutos entre dos fechas con hora
	 * 
	 * @param fecha1 : Fecha 1 que se va a restar
	 * @param fecha2 : Fecha 2 de la que se va a restar
	 * @return : Resultado en minutos
	 * @throws Throwable: Excepcins
	 */
	Double diferenciaMinutos(Date fecha1, Date fecha2) throws Throwable;

	/**
	 * Calcula la diferencia en minutos entre dos fechas con hora usando LocalDateTime
	 * 
	 * @param fecha1 : Fecha 1 que se va a restar
	 * @param fecha2 : Fecha 2 de la que se va a restar
	 * @return : Resultado en minutos
	 * @throws Throwable: Excepcins
	 */
	Double diferenciaMinutosLocal(LocalDateTime fecha1, LocalDateTime fecha2) throws Throwable;

}
