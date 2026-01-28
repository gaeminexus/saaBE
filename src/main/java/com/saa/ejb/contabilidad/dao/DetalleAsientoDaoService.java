/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.DetalleAsiento;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad DetalleAsiento.
 */
@Local
public interface DetalleAsientoDaoService extends EntityDao<DetalleAsiento> {


	/**
	 * Select que recupera las cuentas por mes y anio de una empresa
	 * @param mes			:Mes
	 * @param anio			:Anio
	 * @param empresa		:Id de la empresa
	 * @return				:Listado de resultado
	 * @throws Throwable:Excepcion
	 */
	@SuppressWarnings({ "rawtypes" })
	List selectByMesAnio(Long mes, Long anio, Long empresa) throws Throwable;
	
	/**
	 * Select que recupera el detalle de asiento relacionado a una cuenta en un mes y un anio
	 * @param mes			:Numero de mes
	 * @param anio			:Numero de anio
	 * @param cuenta		:Id de la cuenta
	 * @return				:Detalle de asientos
	 * @throws Throwable	:Excepcion
	 */
	List<DetalleAsiento> selectByCuentaMes(Long mes, Long anio, Long cuenta) throws Throwable;
	
	/**
	 * Recupera el detalle de asiento relacionado a una cuenta
	 * @param idPlanCuenta	:Id de la cuenta
	 * @return				:Detalle de asientos
	 * @throws Throwable	:Excepcion
	 */
	List<DetalleAsiento> selectByIdPlanCuenta(Long idPlanCuenta) throws Throwable; /* creado mely */
	
	/**
	 * Recupera el IdCentroCosto relacionado a una detalle Asiento.
	 * @param idPlanCuenta	:Id de la cuenta
	 * @return				:Detalle de asientos
	 * @throws Throwable	:Excepcion
	 */
	List<DetalleAsiento> selectByIdCentroCosto(Long idCentroCosto ) throws Throwable; /* creado mely */

	/**
	 * Recupera el id del detalle del asiento
	 * @param detalleAsiento
	 * @return
	 * @throws Throwable
	 */
	Long selectByAll(DetalleAsiento detalleAsiento) throws Throwable;
	
	/**
	 * Recupera la suma del debe y el haber de un asiento
	 * @param idAsiento	: Id del asiento
	 * @return			: Lista con la suma del debe y el haber
	 * @throws Throwable: Excepcion
	 */
	@SuppressWarnings({ "rawtypes" })
	List selectSumaDebeHaberByAsiento(Long idAsiento) throws Throwable;
	
	/**
	 * Recupera el detalle de un asiento
	 * @param idAsiento	: Id de asiento
	 * @return			: Listado con el detalle de asiento
	 * @throws Throwable: Excepcion
	 */
	List<DetalleAsiento> selectByIdAsiento(Long idAsiento) throws Throwable;
	
	/**
	 * Recupera los movimientos de una empresa en un mes y anio definido que no se encuentren anulados y 
	 *  que tengan hayan afectado a un centro de costo
	 * @param mes		: Mes
	 * @param anio		: Año
	 * @param empresa	: Id de empresa
	 * @param cenCosto	: Id de centro de costo
	 * @param cuenta	: Id de cuenta contable
	 * @throws Throwable: Excepcion
	 * @return			: Listado de asientos
	 */
	List<DetalleAsiento> selectByPeriodoEstadoAndCc(Long mes, Long anio, Long empresa, Long cenCosto, Long cuenta) throws Throwable;
	
	/**
	 * Recupera los registros de una empresa que se encuentren entre las fechas indicadas y que pertenezcan a una cuenta 
	 * @param empresa	: Id empresa
	 * @param idCuenta	: Id de la Cuenta Contable
	 * @return			: DetalleAsiento
	 * @throws Throwable: Excepcions
	 */
	List<DetalleAsiento> selectByEmpresaCuentaFechas(Long empresa, Long idCuenta, LocalDate fechaInicio, LocalDate fechaFin) throws Throwable;
	
	/**
	 * Obtiene los Movimientos de una Empresa dado Fecha Inicio
	 * @param empresa		: Id de la Empresa
	 * @param fechaInicio	: Fecha Inicio 
	 * @param fechaFin		: Fecha Fin 
	 * @param cuentaInicio	: Cuenta Inicio 
	 * @param cuentaFin		: Cuenta Fin 
	 * @return				: Movimientos
	 * @throws Throwable	: Excepcions
	 */
	List<DetalleAsiento> selectMovimientoByEmpresaCuentaFecha(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, String cuentaInicio, String cuentaFin)throws Throwable;
	
	/**
	 * Obtiene los Movimientos de una Empresa dado un rango de fechas, cuentas, y centro de costo
	 * @param empresa		: Id de la Empresa
	 * @param fechaInicio	: Fecha Inicio 
	 * @param fechaFin		: Fecha Fin 
	 * @param cuentaInicio	: Cuenta Inicio 
	 * @param cuentaFin		: Cuenta Fin 
	 * @param centroInicio	: Centro de costo Inicio 
	 * @param centroFin		: Centro de costo Fin 
	 * @return				: Movimientos
	 * @throws Throwable	: Excepcions
	 */
	List<DetalleAsiento> selectByEmpresaCuentaFechaCentro(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, 
			String cuentaInicio, String cuentaFin, String centroInicio,
			String centroFin) throws Throwable;
	
	
	/**
	 * Recupera la suma del debe y el haber de una cuenta a una fecha dada en una empresa
	 * @param idEmpresa	: Id de la empresa en la que se realiza
	 * @param idCuenta	: Id de la cuenta de la que se desea obtener el saldo
	 * @param fechaDesde: Fecha desde la que se realiza la suma
	 * @param fechaFin	: Fecha hasta la que se realiza la suma
	 * @return			: Suma total del debe y haber
	 * @throws Throwable: Excepcion
	 */
	@SuppressWarnings({ "rawtypes" })
	List selectSumaDebeHaberByFechasEmpresaCuenta(Long idEmpresa, Long idCuenta, LocalDate fechaDesde, LocalDate fechaFin) throws Throwable;
	
	/**
	 * Obtiene los Movimientos en una Empresa de una cuenta dado un rango de fechas y centro de costo
	 * @param idCuenta		: Id de la cuenta contable
	 * @param fechaInicio	: Fecha Inicio 
	 * @param fechaFin		: Fecha Fin
	 * @param centroInicio	: Centro de costo Inicio 
	 * @param centroFin		: Centro de costo Fin 
	 * @return				: Movimientos
	 * @throws Throwable	: Excepcions
	 */
	List<DetalleAsiento> selectByCuentaFechasCentros(Long idCuenta, LocalDate fechaInicio, LocalDate fechaFin, 
			String centroInicio, String centroFin) throws Throwable;
	
	/**
	 * Obtiene los Movimientos en una Empresa de un centro de costo dado un rango de fechas y cuentas
	 * @param idCuenta		: Id de la cuenta contable
	 * @param fechaInicio	: Fecha Inicio 
	 * @param fechaFin		: Fecha Fin
	 * @param cuentaInicio	: Cuenta contable Inicio 
	 * @param cuentaFin		: Cuenta contable Fin 
	 * @return				: Movimientos
	 * @throws Throwable	: Excepcions
	 */
	List<DetalleAsiento> selectByCentroFechasCuentas(Long idCentro, LocalDate fechaInicio, LocalDate fechaFin, 
			String cuentaInicio, String cuentaFin) throws Throwable;
	
	/**
	 * Recupera la suma del debe y el haber de un centro menor a una fecha dada en una empresa
	 * @param idEmpresa	: Id de la empresa en la que se realiza
	 * @param idCentro	: Id del centro de costo del que se desea obtener el saldo
	 * @param fechaDesde: Fecha desde la que se realiza la suma
	 * @param fechaFin	: Fecha hasta la que se realiza la suma
	 * @return			: Suma total del debe y haber
	 * @throws Throwable: Excepcion
	 */
	@SuppressWarnings({ "rawtypes" })
	List selectSumaDebeHaberByFechasEmpresaCentro(Long idEmpresa, Long idCentro, LocalDate fechaDesde, LocalDate fechaFin) throws Throwable;
	
	/**
	 * Recupera la suma del debe y el haber de una cuenta en un centro en un rango de fechas
	 * @param idEmpresa	: Id de la empresa en la que se realiza
	 * @param idCentro	: Id del centro de costo del que se desea obtener el saldo
	 * @param idCuenta	: Id de la cuenta contable de la que se desea obtener el saldo
	 * @param fechaDesde: Fecha desde la que se realiza la suma
	 * @param fechaFin	: Fecha hasta la que se realiza la suma
	 * @return			: Suma total del debe y haber
	 * @throws Throwable: Excepcion
	 */
	@SuppressWarnings({ "rawtypes" })
	List selectSumaDebeHaberByFechasEmpresaCentroCuenta(Long idEmpresa, 
			Long idCentro, Long idCuenta, LocalDate fechaDesde, LocalDate fechaFin) throws Throwable;
	
	/**
	 * Recupera la suma del debe y el haber de una cuenta en un centro hasta una fecha dada
	 * @param idEmpresa	: Id de la empresa en la que se realiza
	 * @param idCentro	: Id del centro de costo del que se desea obtener el saldo
	 * @param idCuenta	: Id de la cuenta contable de la que se desea obtener el saldo
	 * @param fechaHasta: Fecha hasta la que se realiza la suma
	 * @return			: Suma total del debe y haber
	 * @throws Throwable: Excepcion
	 */
	@SuppressWarnings({ "rawtypes" })
	List selectSumaDebeHaberAFechaByEmpresaCentroCuenta(Long idEmpresa, 
			Long idCentro, Long idCuenta, LocalDate fechaHasta) throws Throwable;
	

}
