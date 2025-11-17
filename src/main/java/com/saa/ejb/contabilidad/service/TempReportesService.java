package com.saa.ejb.contabilidad.service;

import java.util.Date;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.TempReportes;

import jakarta.ejb.Remote;

@Remote
public interface TempReportesService extends EntityService<TempReportes>  {
	

	 /**
	 * Elimina todas las cuentas de una secuencia de ejecucion
	 * @param codigo		: Id de ejecucion del reporte 
	 * @throws Throwable	: Excepcion
	 */
	 void removeBySecuencia (Long codigo) throws Throwable;
	
	/**
	 * Recupera entidad con el id
	 * @param id		: Id de la entidad
	 * @return			: Recupera entidad
	 * @throws Throwable: Excepcion
	 */
	 TempReportes selectById(Long id) throws Throwable;	
	
	/**
	 * Inserta cuentas contables de empresa que se encuentran en la parametrizacion
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio 
	 * @param cuentaFin		: Cuenta Fin
	 * @param idEjecucion	: Id de Ejecucion del Reporte
	 * @throws Throwable	: Excepcion
	 */
	void insertaCuentas (Long empresa, String cuentaInicio, String cuentaFin, Long idEjecucion) throws Throwable;
	
	/**
	 * Actualiza Movimientos Debe Haber de cada Cuenta Contable de DTMT
	 * @param empresa		: Id de la empresa
	 * @param fechaInicio	: Fecha Inicio
	 * @param fechaFin		: Fecha Fin
	 * @param idEjecucion	: Id de la Ejecucion
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @throws Throwable	: Excepcions
	 */
	void actualizaDebeHaberMovimiento(Long empresa, Date fechaInicio, Date fechaFin, Long idEjecucion
			  , int acumulacion) throws Throwable;
	
	/**
	 * Actualiza las Cuentas Padres con los valores del Debe Haber Saldos
	 * @param idEjecucion	: Id de la Ejecucion
	 * @throws Throwable	: Excepcions
	 */
	void actualizaSaldosAcumulacion(Long idEjecucion)throws Throwable;
	
	/** 
	 * Procedimiento Reporte de Balances por Rango de Fechas
	 * @param fechaFin		: Fecha Fin 
	 * @param fechaInicio	: Fecha Inicio 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno 
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @return				: Id de Ejecucion 
	 * @throws Throwable	: Excepcions
	 */
	Long reporteRangoFecha (Date fechaFin, Date fechaInicio, Long empresa, Long codigoAlterno, int acumulacion) throws Throwable;

	/**
	 * Metodo para copiar Plan desde la Parametrizacion 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno
	 * @param idEjecucion	: Id de la Ejecucion del Reporte
	 * @throws Throwable	: Excepcions
	 */
	void copiaPlanParametrizacion (Long empresa, Long codigoAlterno, Long idEjecucion)throws Throwable;
	
	/** 
	 * Procedimiento Reporte de Balances por Rango de Fechas que incluye los centros de costo
	 * @param fechaFin		: Fecha Fin 
	 * @param fechaInicio	: Fecha Inicio 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno 
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @return				: Id de Ejecucion 
	 * @throws Throwable	: Excepcions
	 */
	Long reporteCentroRangoFecha (Date fechaFin, Date fechaInicio, Long empresa, Long codigoAlterno, int acumulacion) throws Throwable;
	
	/**
	 * Metodo para copiar Plan con datos de centro de costo desde la Parametrizacion 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno
	 * @param idEjecucion	: Id de la Ejecucion del Reporte
	 * @throws Throwable	: Excepcions
	 */
	void copiaPlanCentroParametrizacion (Long empresa, Long codigoAlterno, Long idEjecucion)throws Throwable;
	
	/**
	 * Inserta cuentas contables de empresa parametrizadas por centro de costo
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio 
	 * @param cuentaFin		: Cuenta Fin
	 * @param idEjecucion	: Id de Ejecucion del Reporte
	 * @throws Throwable	: Excepcion
	 */
	void insertaCuentasByCentro (Long empresa, String cuentaInicio, String cuentaFin, Long idEjecucion) throws Throwable;
	
	/**
	 * Actualiza Movimientos Debe Haber de cada Cuenta Contable por centro de costo de DTMT 
	 * @param empresa		: Id de la empresa
	 * @param fechaInicio	: Fecha Inicio
	 * @param fechaFin		: Fecha Fin
	 * @param idEjecucion	: Id de la Ejecucion 
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @throws Throwable	: Excepcions
	 */
	void actualizaDebeHaberMovimientoCentro(Long empresa, Date fechaInicio, Date fechaFin, Long idEjecucion
			  , int acumulacion) throws Throwable;

	/**
	 * Actualiza las Cuentas Padres con los valores del Debe Haber Saldos
	 * @param idEjecucion	: Id de la Ejecucion
	 * @throws Throwable	: Excepcions
	 */
	void actualizaSaldosAcumulacionCentro(Long idEjecucion)throws Throwable;
	
	/** 
	 * Procedimiento Reporte de Balances por Rango de Fechas distribuido por plan de cuentas por centro de costo
	 * @param fechaFin		: Fecha Fin 
	 * @param fechaInicio	: Fecha Inicio 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno 
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @return				: Id de Ejecucion 
	 * @throws Throwable	: Excepcions
	 */
	Long reportePlanCentroRangoFecha (Date fechaFin, Date fechaInicio, Long empresa, Long codigoAlterno, int acumulacion) throws Throwable;
	
	/**
	 * Metodo para copiar centro de costo desde la Parametrizacion para el reporte distribuido de Plan de cuentas por centro de costo
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno
	 * @param idEjecucion	: Id de la Ejecucion del Reporte
	 * @throws Throwable	: Excepcions
	 */
	void copiaPlanByCentro (Long empresa, Long codigoAlterno, Long idEjecucion)throws Throwable;
	
	/**
	 * Inserta centros de costo de empresa parametrizados por cuenta contable
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio 
	 * @param cuentaFin		: Cuenta Fin
	 * @param idEjecucion	: Id de Ejecucion del Reporte
	 * @throws Throwable	: Excepcion
	 */
	void insertaCentroByCuentas(Long empresa, String cuentaInicio, String cuentaFin, Long idEjecucion) throws Throwable;
	
	/**
	 * Actualiza Movimientos Debe Haber de cada Centro de costo por cuenta contable
	 * @param empresa		: Id de la empresa
	 * @param fechaInicio	: Fecha Inicio
	 * @param fechaFin		: Fecha Fin
	 * @param idEjecucion	: Id de la Ejecucion 
	 * @param acumulacion	: 1 Acumulacion, 0 sin acumulacion 
	 * @throws Throwable	: Excepcions
	 */
	void actualizaDebeHaberMovimientoPlanByCentro(Long empresa, Date fechaInicio, Date fechaFin, Long idEjecucion
			  , int acumulacion) throws Throwable;
	
	/**
	 * Elimina todos los movimientos del reporte que esten en cero
	 * @param idEjecucion	: Id de ejecución del reporte
	 * @throws Throwable	: Excepcion
	 */
	void eliminaMovimientosEnCero(Long idEjecucion) throws Throwable;
	
}
