/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;

import java.util.Date;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.PlanCuenta;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad PlanCuenta.
 */ 
@Local
public interface PlanCuentaDaoService extends EntityDao<PlanCuenta> {

	/**
	 * Elimina el plan de cuenta de una empresa
	 * @param empresa		: Id de la empresa de la que se eliminara el plan
	 * @throws Throwable	: Excepcion
	 */
	void deleteByEmpresa(Long empresa) throws Throwable;
	
	/**
	 * Recupera el plan de cuentas de una empresa
	 * @param empresa		: Id de la empresa de la que se recupera el plan
	 * @return				: Lista de cuentas que contiene el plan de cuenta
	 * @throws Throwable	: Excepcion
	 */
	List<PlanCuenta> selectByEmpresa(Long empresa) throws Throwable;
	
	/**
	 * Recupera las cuentas de una empresa que pertenecen a una naturaleza
	 * @param empresa	: Id de la empresa
	 * @param naturaleza: Numero de naturaleza
	 * @return			: Listado de cuentas
	 * @throws Throwable: Excepcion
	 */
	List<PlanCuenta> selectByEmpresaNaturaleza(Long empresa, Long naturaleza) throws Throwable;
	
	/**
	 * Recupera las cuentas de una empresa que pertenecen a una naturaleza
	 * @param empresa	: Id de la empresa
	 * @param naturaleza: Numero de naturaleza
	 * @return			: Listado de cuentas
	 * @throws Throwable: Excepcion
	 */
	List<PlanCuenta> selectByNaturalezaCuenta( Long naturaleza) throws Throwable;
	
	/**
	 * Recupera las cuentas de una empresa que manejan centro de costo
	 * @param empresa	: Id de la empresa
	 * @return			: Listado de cuentas que cumplen la condicion
	 * @throws Throwable: Excepcion
	 */
	List<PlanCuenta> selectByEmpresaManejaCC(Long empresa) throws Throwable;
	
	/**
	 * Recupera por el numero de cuenta y el id de empresa
	 * @param cuenta	: Numero de cuenta contable
	 * @param empresa	: Id de empresa
	 * @return			: Registro recuperado
	 * @throws Throwable: Excepcion
	 */
	PlanCuenta selectByCuentaEmpresa(String cuenta, Long empresa) throws Throwable;
	
	/**
	 * Metodo recupera la maxima cuenta de una empresa dado la cuneta contable
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Contable
	 * @param siguienteNaturaleza: Siguiente naturaleza de cuenta
	 * @return				: Maxima Cuenta
	 * @throws Throwable	: Excepcions
	 */
	String selectMaximaByCuenta (Long empresa, String cuentaInicio, String siguienteNaturaleza) throws Throwable;
	
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
	List<PlanCuenta> selectMovimientoByEmpresaCuentaFecha(Long empresa, Date fechaInicio, Date fechaFin, String cuentaInicio, String cuentaFin)throws Throwable;
	
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
	List<PlanCuenta> selectByEmpresaCuentaFechaCentro(Long empresa, Date fechaInicio, Date fechaFin, 
			String cuentaInicio, String cuentaFin, String centroInicio,
			String centroFin) throws Throwable;
	
	/**
	 * Recupera todas las cuentas que tengan el mismo número de cuenta seguido de un punto para ver cuáles son hijos
	 * @param numeroCuenta	: Número de cuenta a buscar
	 * @param idEmpresa		: Id de la empresa
	 * @return				: Listado de cuentas hijas de una cuenta
	 * @throws Throwable	: Excepcion
	 */
	List<PlanCuenta> selectHijosByNumeroCuenta(String numeroCuenta, Long idEmpresa) throws Throwable;
	
	/**
	 * select que obtiene un rango de cuentas activas de una empresa 
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio
	 * @param cuentaHasta	: Cuenta Hasta
	 * @return				: Plan Cuenta
	 * @throws Throwable	: Excepcion
	 */
	List<PlanCuenta> selectByRangoEmpresaEstado (Long empresa, String cuentaInicio, String cuentaHasta)throws Throwable;
	
	/**
	 * select que obtiene un rango de cuentas de movimiento activas de una empresa 
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio
	 * @param cuentaHasta	: Cuenta Hasta
	 * @return				: Plan Cuenta
	 * @throws Throwable	: Excepcion
	 */
	List<PlanCuenta> selectMovimientoByRangoEmpresaEstado (Long empresa, String cuentaInicio, String cuentaHasta)throws Throwable;
	

	 
	/**
	 * Recupera Cuentas en un Rango por Empresa
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio
	 * @param cuentaFin		: Cuenta Fin
	 * @return				: Rango de Cuentas
	 * @throws Throwable	: Excepcions
	 */
	List<PlanCuenta> selectCuentasByRango(Long empresa, String cuentaInicio, String cuentaFin)throws Throwable;
	
	/**
	 * Recupera la maxima cuenta segun el numero de cuenta
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Numero de Cuenta
	 * @return				: Cuenta maxima
	 * @throws Throwable	: Excepciones
	 */
	String selectMaxCuentaByNumeroCuenta(Long empresa, String cuenta)throws Throwable;
	
	/**
	 * Recupera todas las cuentas contables de una empresa menos la raiz
	 * @param empresa		: Id de la empresa
	 * @return				: Listado de cuentas de empresa
	 * @throws Throwable	: Excepcion
	 */
	List<PlanCuenta> selectByEmpresaSinRaiz(Long empresa) throws Throwable;
	
	/**
	 * Recupera el nivel RAIZ de la empresa
	 * @param empresa	: Id de empresa
	 * @return			: Nivel raiz de la empresa
	 * @throws Throwable: Excepcion
	 */
	PlanCuenta selectRaizByEmpresa(Long empresa) throws Throwable;
	
	/**
	 * Recupera el numero de registros activos que tienen el mismo id Padre
	 * @param idPadre	: Id de la cuenta padre
	 * @return			: Numero de registros activos con el idPadre del parametro
	 * @throws Throwable: Excepcion
	 */
	int numeroRegActivosByIdPadre(Long idPadre) throws Throwable;
	
	/**
	 * Recupera todas las cuentas contables activas de una empresa
	 * @param empresa		: Id de la empresa
	 * @return				: Listado de cuentas activas de empresa
	 * @throws Throwable	: Excepcion
	 */
	List<PlanCuenta> selectActivasByEmpresa(Long empresa) throws Throwable;
	
	/**
	 * Recupera el listado de cuentas que tienen el mismo padre
	 * @param idPadre		: Id del la cuenta padre
	 * @return				: Lista de cuentas que tienen el mismo padre
	 * @throws Throwable	: Excepcion
	 */
	List<PlanCuenta> selectByIdPadre(Long idPadre) throws Throwable;

}
