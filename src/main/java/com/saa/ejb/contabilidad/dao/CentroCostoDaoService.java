/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.CentroCosto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad CentroCosto.
 */
@Local
public interface CentroCostoDaoService extends EntityDao<CentroCosto> {

	/**
	 * Elimina los centros de costo de una empresa
	 * @param empresa		:Id de la empresa de la que se eliminara los centros de costo
	 * @throws Throwable	:Excepcion
	 */
	void deleteByEmpresa(Long empresa) throws Throwable;
	
	/**
	 * Recupera centro de costos activos de movimiento por empresa 
	 * @param empresa	: Id de empresa	
	 * @return			: Listado de registros recuperados
	 * @throws Throwable: Excepcion
	 */
	List<CentroCosto> selectMovimientosByEmpresa(Long empresa) throws Throwable;	
	
	/**
	 * Recupera por el numero de cuenta y el id de empresa
	 * @param cuenta	: Numero de centro de costo
	 * @param empresa	: Id de empresa
	 * @return			: Registro recuperados
	 * @throws Throwable: Excepcion
	 */
	CentroCosto selectByCuentaEmpresa(String cuenta, Long empresa) throws Throwable;
	
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
	List<CentroCosto> selectByEmpresaCuentaFechaCentro(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, 
			String cuentaInicio, String cuentaFin, String centroInicio,
			String centroFin) throws Throwable;
	
	/**
	 * Recupera todas los centros que tengan el mismo número de centro seguido de un punto para ver cuáles son hijos
	 * @param numeroCentro	: Número de centro a buscar
	 * @param idEmpresa		: Id de la empresa
	 * @return				: Listado de centros hijos
	 * @throws Throwable	: Excepcion
	 */
	List<CentroCosto> selectHijosByNumeroCuenta(String numeroCentro, Long idEmpresa) throws Throwable;
	
	/**
	 * Recupera todas los centros de una empresa menos el raiz
	 * @param idEmpresa		: Id de la empresa
	 * @return				: Listado de centros hijos
	 * @throws Throwable	: Excepcion
	 */
	List<CentroCosto> selectByEmpresaSinRaiz(Long idEmpresa) throws Throwable;
	
	/**
	 * Recupera el nivel RAIZ de la empresa
	 * @param empresa	: Id de empresa
	 * @return			: Nivel raiz de la empresa
	 * @throws Throwable: Excepcion
	 */
	CentroCosto selectRaizByEmpresa(Long empresa) throws Throwable;
	
	/**
	 * Recupera el numero de registros activos que tienen el mismo id Padre
	 * @param idPadre	: Id de la cuenta padre
	 * @return			: Numero de registros activos con el idPadre del parametro
	 * @throws Throwable: Excepcion
	 */
	int numeroRegActivosByIdPadre(Long idPadre) throws Throwable;
	
	/**
	 * Recupera todos los centros que tengan el mismo codigo de padre
	 * @param idPadre		: Id del centro padre
	 * @return				: Listado de centros que tienen el mismo codigo de padre
	 * @throws Throwable	: Excepcion
	 */
	List<CentroCosto> selectByIdPadre(Long idPadre) throws Throwable;
	
	/**
	 * Recupera todos los centros de una empresa
	 * @param IdEmpresa		: Id de la empresa
	 * @return				: Listado de centros de una empresa
	 * @throws Throwable	: Excepcion
	 */
	List<CentroCosto> selectByEmpresa(Long idEmpresa) throws Throwable;
	
	/**
	 * Recupera todos los centros activos de una empresa
	 * @param IdEmpresa		: Id de la empresa
	 * @return				: Listado de centros activos de una empresa
	 * @throws Throwable	: Excepcion
	 */
	List<CentroCosto> selectActivosByEmpresa(Long idEmpresa) throws Throwable;
	
	
}
