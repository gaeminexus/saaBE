package com.saa.ejb.cnt.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.TempReportes;

import jakarta.ejb.Local;

@Local
public interface TempReportesDaoService  extends EntityDao<TempReportes>  {
	
	/**
	 * Recupera todas las cuentas que pertenecen a una secuencia de ejecucion
	 * @param codigo		: Id de ejecucion del reporte 
	 * @throws Throwable	: Excepcion 
	 */
	List<TempReportes> selectBySecuencia(Long codigo)throws Throwable;
	
	/**
	 * Select TempReportes por el Nivel
	 * @param idEjecucion	: Id del la Ejecucion
	 * @return				: Nivel
	 * @throws Throwable	: Excepcions
	 */
	@SuppressWarnings("rawtypes")
	List selectNivelesByIdEjecucion(Long idEjecucion) throws Throwable;
	
	/**
	 * Select TempReportes por la CuentaPadre
	 * @param idEjecucion	: Id Ejecucion
	 * @param nivel			: nivel
	 * @return				: CodigoPadre
	 * @throws Throwable	: Excepcions
	 */
	@SuppressWarnings("rawtypes")
	List selectPadresByNivel(Long idEjecucion, Long nivel )throws Throwable;
	
	/**
	 * Select TempReportes por la suma de las Cuentas
	 * @param idEjecucion	: Id de la Ejecucion
	 * @param cuentaPadre	: Codigo del Padre
	 * @param nivel			: Nivel 
	 * @return				: Suma Cuentas 
	 * @throws Throwable	: Excepcions
	 */
	List<TempReportes> selectByCuentaPadreNivel(Long idEjecucion, Long cuentaPadre, Long nivel)throws Throwable;
	
	/** 
	 * Select TempReportes para la Actualizacion de Saldos Anteriores 
	 * @param idEjecucion	: Id de la Ejecucion
	 * @param tipo			: Tipo
	 * @return				: Lista del TempReportes
	 * @throws Throwable	: Excepcions
	 */
	List<TempReportes> selectMovimientosByIdEjecucion(Long idEjecucion)throws Throwable;
	
	/**
	 * Obtiene una Secuencia del Reporte
	 * @return			: Numero se Secuencia
	 * @throws Throwable: Excepcions
	 */
	Long obtieneSecuenciaReporte()throws Throwable;
	
	/**
	 * Recupera el registro de una secuencia de ejecucion con el id de la cuenta contable
	 * @param idEjecucion	: Id de ejecucion de reporte
	 * @param idCuenta		: Id de la cuenta contable
	 * @return				: Registro
	 * @throws Throwable	: Excepcion
	 */
	TempReportes selectByIdEjecucionCuenta(Long idEjecucion, Long idCuenta) throws Throwable;
	
	/**
	 * Select de los diferentes numeros de centros de costo por el id de ejecucion
	 * @param idEjecucion	: Id del la Ejecucion
	 * @return				: Numeros de centros de costo incluidos en el reporte
	 * @throws Throwable	: Excepcions
	 */
	@SuppressWarnings("rawtypes")
	List selectNumerosCentroByIdEjecucion(Long idEjecucion) throws Throwable;
	
	/**
	 * Recupera los niveles por id de ejecucion y por numero de centro de costo
	 * @param idEjecucion	: Id del la Ejecucion
	 * @param numeroCentro	: Numero de centro de costo
	 * @return				: Nivel
	 * @throws Throwable	: Excepcions
	 */
	@SuppressWarnings("rawtypes")
	List selectNivelesByCentroIdEjecucion(Long idEjecucion, String numeroCentro) throws Throwable;	
	
	/**
	 * Select TempReportes por la CuentaPadre
	 * @param idEjecucion	: Id Ejecucion
	 * @param nivel			: nivel
	 * @param numeroCentro	: Numero de centro de costo
	 * @return				: CodigoPadre
	 * @throws Throwable	: Excepcions
	 */
	@SuppressWarnings("rawtypes")
	List selectPadresByNivelCentro(Long idEjecucion, Long nivel, String numeroCentro)throws Throwable;
	
	/**
	 * Select TempReportes por la suma de las Cuentas
	 * @param idEjecucion	: Id de la Ejecucion
	 * @param cuentaPadre	: Codigo del Padre
	 * @param nivel			: Nivel 
	 * @param numeroCentro	: Numero de centro de costo
	 * @return				: Suma Cuentas 
	 * @throws Throwable	: Excepcions
	 */
	@SuppressWarnings("rawtypes")
	List selectByCuentaPadreNivelCentro(Long idEjecucion, Long cuentaPadre, 
			Long nivel, String numeroCentro)throws Throwable;
	
	/**
	 * Recupera el registro de una secuencia de ejecucion con el id de la cuenta contable y el numero de centro de costo
	 * @param idEjecucion	: Id de ejecucion de reporte
	 * @param idCuenta		: Id de la cuenta contable
	 * @param numeroCentro	: Numero de centro de costo
	 * @return				: Registro
	 * @throws Throwable	: Excepcion
	 */
	TempReportes selectByIdEjecucionCuentaCentro(Long idEjecucion, Long idCuenta, String numeroCentro) throws Throwable;
	
	/**
	 * Elimina los registros de un id de ejecución que tengan cero en los saldos 
	 * @param idEjecucion	: Id de ejecución de reporte
	 * @throws Throwable	: Excepcion
	 */
	void deleteBySaldosCeroIdEjecucion(Long idEjecucion) throws Throwable;
	
}
