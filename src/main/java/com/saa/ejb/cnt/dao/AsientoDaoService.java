/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.cnt.dao;

import java.util.Date;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.Asiento;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad Asiento.
 */
@Local
public interface AsientoDaoService extends EntityDao<Asiento> {

	/**
	 * Recupera los anios de los asientos de una empresa
	 * @param empresa	: Id de la empresa a buscar
	 * @return			: Objeto con anios recuperados
	 * @throws Throwable: Excepcion
	 */	
	List<Long> selectAniosAsiento(Long empresa) throws Throwable;
	
	/**
	* Recupera los usuarios que han  ingresados asientos de una empresa
	* @param empresa	: Id de la empresa a buscar
	* @return			: Objeto con anios recuperados
	* @throws Throwable: Excepcion
	*/
	List<String> selectUsuario(Long empresa) throws Throwable;
	
	/**
	 * Recupera el listado de todos los asientos que han sido usados para reversar otros
	 * @param empresa	: Id de la empresa en la que se realiza la búsqueda
	 * @return			: Listado con resultado de select
	 * @throws Throwable: Excepcion
	 */
	List<Asiento> selectAsientoReverso(Long empresa) throws Throwable;
	
	/**
	 * Select que recupera los asientos por mes y anio de una empresa
	 * @param mes		: Mes
	 * @param anio		: Anio
	 * @param empresa	: Id de la empresa
	 * @return			: Listado de resultado
	 * @throws Throwable: Excepcion
	 */
	List<Asiento> selectByMesAnio(Long mes, Long anio, Long empresa) throws Throwable;
	
	/**
	 * Recupera el maximo numero de asiento de acuerdo a un tipo y de una empresa
	 * @param tipo		: Id de tipo de asiento
	 * @param empresa	: Id de empresa
	 * @return			: Listado que contiene maximo numero de asiento
	 * @throws Throwable: Excepcion
	 */
	List<Asiento> selectMaxNumero(Long tipo, Long empresa) throws Throwable;
	
	/**
	 * Select para recuperar el asiento de cierre de un periodo de una empresa
	 * @param mes		: Mes del asiento
	 * @param anio		: Anio del asiento
	 * @param tipo		: Id del tipo de asiento
	 * @param empresa	: Id de la empresa
	 * @return			: Asiento
	 * @throws Throwable: Excepcion
	 */
	Asiento selectAsientoCierre(Long mes, Long anio, Long tipo, Long empresa) throws Throwable;
	
	/**
	 * Recupera un asiento por numero, empresa y tipo
	 * @param numero	: Numero de asiento
	 * @param empresa	: Id de la empresa
	 * @param tipo		: Id de tipo de asiento
	 * @return			: Asiento recuperado
	 * @throws Throwable: Excepcion
	 */
	Asiento selectByNumeroEmpresaTipo(Long numero, Long empresa, Long tipo) throws Throwable;
	
	/**
	 * Recupera por codigo de mayorizacion
	 * @param idMayorizacion	: Id de mayorizacion
	 * @return					: Registros de asiento
	 * @throws Throwable		: Excepcion
	 */
	List<Asiento> selectByMayorizacion(Long idMayorizacion) throws Throwable;
	
	/**
	 * Recupera Datos de Consulta de Asientos
	 * @param idEmpresa				: Id de laEmpresa
	 * @param tipoAsiento			: Tipo de Asiento
	 * @param numero				: Numero
	 * @param estado				: Estado
	 * @param nombreUsuario			: Nombre del usuario 
	 * @param rubroModuloClienteH	: Rubro Modulo 
	 * @param idReversion			: Id Reversion
	 * @param numeroAnio			: Numero Anio
	 * @param numeroMes				: Numero Mes
	 * @param fechaIngresoDesde		: Fecha Ingreso Desde
	 * @param fechaIngresoHasta		: Fecha Ingreso Hasta
	 * @param fechaAsientoDesde  	: Fecha Asiento Desde
	 * @param fechaAsientoHasta		: Fecha Asiento Hasta
	 * @return						: Datos de Consulta
	 * @throws Throwable			: Excepcions 
	 */
	
	List<Asiento> selectByConsultaCamposAsiento (Long idEmpresa, Long tipoAsiento, Long numero,
												 Long estado, String nombreUsuario, Long rubroModuloClienteH,
												 Long idReversion, Long numeroAnio, Long numeroMes,
												 Date fechaIngresoDesde,
												 Date fechaIngresoHasta, Date fechaAsientoDesde, Date fechaAsientoHasta
												 )throws Throwable;
	
	/**
	 * @param idPeriodo	: Id del periodo
	 * @return		: Listado de asientos por periodo
	 * @throws Throwable	: Excepcion
	 */
	List<Asiento> selectByIdPeriodo(Long idPeriodo) throws Throwable;
	
	
}
