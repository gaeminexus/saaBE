/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.Periodo;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad Periodo.
 */
@Local 
public interface PeriodoDaoService extends EntityDao<Periodo> {

	/**
	* Recupera los Periodos de una empresa
	* @param empresa		:Id de la empresa a buscar
	* @return				:Objeto con anios recuperados
	* @throws Throwable		:Excepcion
	*/
	List<String> selectPeriodo(Long empresa) throws Throwable;
	
	/**
	 * Obtiene el periodo inicial para Mayorizacion y final para Desmayorizacion
	 * @param proceso		:Indica el proceso a realizar. 1 = Mayorizar, 2 = Desmayorizar
	 * @param empresa		:Id de la empresa
	 * @return				:Id del periodo resultado
	 * @throws Throwable	:Excepcion
	 */
	List<Long> periodoMayorizacionDesmayorizacion(int proceso, Long empresa) throws Throwable;
	
	/**
	 * Select para obtener los peridos en un rango de codigos dentro de una empresa
	 * @param empresa		:Id de la empresa
	 * @param periodoInicia	:Id del periodo inicial
	 * @param periodoFin	:Id del periodo final
	 * @param proceso		:Indica el proceso a realizar. 1 = Mayorizar, 2 = Desmayorizar
	 * @return				:Listado de periodos
	 * @throws Throwable	:Excepcion
	 */
	List<Periodo> selectRangoPeriodos(Long empresa, Long periodoInicia, Long periodoFin, int proceso) throws Throwable;

	/**
	 * Select para recuperar el anio con el id de la empresa
	 * @param empresa		:Id de la Empresa
	 * @return				:Anios recuperados
	 * @throws Throwable	:Excepcion 
	 */
	List<Long> selectRecuperaAnio (Long empresa) throws Throwable;  
	
	/**
	 * Recupera un periodo con empresa, mes y año
	 * @param empresa	: Id de empresa
	 * @param mes		: Numero de mes
	 * @param anio		: Numero de año
	 * @return			: Estado recuperado
	 * @throws Throwable: Excepcion
	 */
	Periodo selectByMesAnioEmpresa(Long empresa, Long mes, Long anio) throws Throwable;
	
	/**
	 * Recupera un perido con empresa y maxima fecha
	 * @param empresa	: Id de la Empresa
	 * @return			: Fecha Maxima
	 * @throws Throwable: Excepcions
	 */
	Periodo selectByEmpresaMaxFecha(Long empresa) throws Throwable;
	
	/**
	 * Select que recupera la Mayorizacion y Desmayorizacion con el Periodo
	 * @param idPeriodo	: Id del Periodo
	 * @return			: Id Mayorizacion y Id Desmayorizacion
	 */
	List<Periodo> selectByPeriodo(Long idPeriodo)throws Throwable;
	
	/**
	 * Proceso que devuelve el maximo periodo en cualquier estado anterior a una fecha
	 * @param empresa	: Id de la Empresa 
	 * @param estado	: Estado de periodo a buscar
	 * @param fechaFin	: Fecha a la que se desea buscar el periodo
	 * @return			: Id del Periodo
	 * @throws Throwable: Excepcions
	 */
	Periodo selectMaximoAnteriorByEstadoEmpresa(Long empresa, int estado, LocalDate fecha)throws Throwable;
	
	/**
	 * Recupera el periodo que pertenece a una fecha
	 * @param fecha		: Fecha de la que se desea obtener el periodo
	 * @param empresa	: Id de la empresa en la que desea buscar
	 * @return			: Periodo correspondiente a la fecha
	 * @throws Throwable: Excepcion
	 */
	Periodo selectByFecha(LocalDate fecha, Long empresa) throws Throwable;
	
	/**
	 * Proceso que devuelve el minimo periodo en cualquier estado anterior a una fecha
	 * @param empresa	: Id de la Empresa 
	 * @param estado	: Estado de periodo a buscar
	 * @param fechaFin	: Fecha a la que se desea buscar el periodo
	 * @return			: Id del Periodo
	 * @throws Throwable: Excepcions
	 */
	Periodo selectMinimoAnteriorByEstadoEmpresa(Long empresa, int estado, LocalDate fecha)throws Throwable;
	
	/**
	 * Recupera todos los periodos de una empresa
	 * @param idEmpresa	: Id de la empresa
	 * @return			: Periodos que pertenecen a una empresa
	 */
	List<Periodo> selectByEmpresa(Long idEmpresa)throws Throwable;
	
	/**
	 * Recupera el maximo periodo mayorizado anterior a un periodo dado
	 * @param periodo	: Id de periodo
	 * @param idEmpresa	: Id de la empresa
	 * @return			: Maximo Anterior mayorizado 
	 */
	List<Periodo> selectAnteriorMayorizado(Long periodo, Long empresa)throws Throwable;
	
	/**
	 * Recupera el maximo periodo anterior a otro sin importar el estado
	 * @param periodo	: Id de periodo
	 * @param idEmpresa	: Id de la empresa
	 * @return			: Máximo periodo anterior
	 */
	List<Periodo> selectPeriodoAnterior(Long periodo, Long empresa)throws Throwable;
}
