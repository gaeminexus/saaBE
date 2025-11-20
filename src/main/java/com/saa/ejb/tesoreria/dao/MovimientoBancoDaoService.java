/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.dao;

import java.util.Date;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tesoreria.Conciliacion;
import com.saa.model.tesoreria.MovimientoBanco;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice MovimientoBanco.  
 */
@Local
public interface MovimientoBancoDaoService extends EntityDao<MovimientoBanco>{
	
	/**
	 * Metodo que Obtiene la Fecha del Primer Movimiento de una Cuenta Bancaria
	 * @param idCuenta		: Id de la Cuenta  para la Busqueda
	 * @return				: Fecha del Primer Movimiento
	 * @throws Throwable	: Exepcions
	 */
	 String obtieneFechaPrimerMovimiento (Long idCuenta) throws Throwable; 
	
	/**
	 * Metodo para Recuperar los movimientos en transito de la cuenta bancaria en rango de fecha
	 * @param idCuenta			: Id de la cuenta para la busqueda 
	 * @param fechaInicio		: Fecha Inicial  
	 * @param fechaFin			: Fecha Finnal
	 * @return					: Movimientos
	 * @throws Throwable		: Exepciones
	 */
	 Double recuperaValorTrancitoCuentaBancaria(Long idCuenta, Date fechaInicio, Date fechaFin)throws Throwable;
	
	/**
	 * Recupera la lista de movimientos bancarios por asiento 
	 * @param idAsiento	: Id de asiento
	 * @return			: lista de movimientos bancarios
	 * @throws Throwable: Excepcion
	 */
	 List<MovimientoBanco> selectByAsiento(Long idAsiento) throws Throwable;
	 
	/**
	 * Recupera Cuentas con id Periodo
	 * @param idPeriodo	: Id del Periodo
	 * @return			: Cuentas
	 * @throws Throwable: Excepcions
	 */
	List<MovimientoBanco> selectCuentasByIdPeriodo(Long idPeriodo)throws Throwable;
	
	/**
	 * Recupera la suma de valor filtrada por cuenta, perido, tipo de movimiento, no conciliados y activos
	 * @param idCuenta				: Id Cuenta
	 * @param idPeriodo				: Id Periodo
	 * @param codigoAlternoRubro	: Codigo Alterno Rubro
	 * @param codigoAlternoDetalle	: Codigo Alterno Detalle
	 * @param conciliado			: Indica si esta conciliado 0 = NO, 1 = SI
	 * @param estado				: Estado
	 * @return						: Registros que cumplen la condicion
	 * @throws Throwable			: Excepcions
	 */
	Double selectSumValorByCuentaPeriodoTipoConciliadoEstado
		(Long idCuenta, Long idPeriodo, int codigoAlternoRubro, 
		 int codigoAlternoDetalle, int conciliado, int estado) throws Throwable;
	
	/**
	 * Recupera el Valor con RubroMovimiento en DepositoTrancito y DepositoConfirmado
	 * @param empresa				: Id de al Empresa
	 * @param cuentaBancaria		: Id de la Cuenta Bancaria 
	 * @param periodo				: Id del Periodo
	 * @param rubroTipoMovimientoH	: RubroTipoMovimientoH 
	 * @return						: Valor
	 * @throws Throwable			: Excepcions
	 */
	Double selectMovimientoBancarioByTipo (Long empresa, Long cuentaBancaria, Long periodo, Long tipoMovimiento1, Long tipoMovimiento2, Long estado)throws Throwable;
	
	//busca los movimientos en trnasito de la cuenta bancaria en rango de fecha 
	//OBTENER SALDOS DE CUENTA BANCARIA POR RANGO DE FECHAS
	/**
	 * Obtiene Saldos de Cuenta Bancaria por Rango de Fechas
	 * @param idCuenta			: Id de la Cuenta
	 * @param fechaInicio		: Fecha Inicio 
	 * @param fechaFin			: Fecha Fin
	 * @param codigoAlternoRubro: Codigo Alterno Rubro
	 * @param tipoMovimiento1	: Tipo de  Movimiento1 
	 * @param tipoMovimiento2	: Tipo de  Movimiento2
	 * @param tipoMovimiento3	: Tipo de  Movimiento3
	 * @param estado			: Estado
	 * @return					: Saldos de Cuentas
	 * @throws Throwable		: Excepcions
	 */
	Long selectSaldosCuentaByRangoFechas(Long idCuenta, Date fechaInicio, Date fechaFin, 
										 Long codigoAlternoRubro, Long tipoMovimiento1, Long tipoMovimiento2, 
										 Long tipoMovimiento3, Long estado)throws Throwable;
	
	/**
	 * Recupera registros para conciliar 
	 * @param idCuentaBancaria	: Id de la Cuenta Bancaria
	 * @param fecha				: Se obtienen los registros menores a esta fecha
	 * @return					: Numero de registros encontrados
	 * @throws Throwable		: Excepcions
	 */
	 Long cuentaByCuentaBancariaEstadoMenorAFecha(Long idCuentaBancaria, Date fecha) throws Throwable;
	 
	 /**
	 * Recupera registros para conciliar 
	 * @param idCuentaBancaria	: Id de la Cuenta Bancaria
	 * @param fecha				: Se obtienen los registros menores a esta fecha
	 * @return					: Registros que estan en estado activo y que no se encuentran en ninguna conciliacion
	 * @throws Throwable		: Excepcions
	 */
	 List<MovimientoBanco> selectSinConsByCuentaEstadoMenorAFecha
	 					(Long idCuentaBancaria, Date fecha) throws Throwable;
	 
	 /**
	  * Actualiza el estado, fecha de conciliacion y id de conciliacion
	 * @param idMovimiento		: Id del movimiento de conciliacion
	 * @param fechaConciliacion	: Fecha en la que se concilia el movimiento
	 * @param conciliacion		: Conciliacion en la que se incluye el movimiento
	 * @param estado			: Estado del movimiento
	 * @throws Throwable		: Excepcion
	 */
	void updateEstadoFechaConciliaById(Long idMovimiento, Date fechaConciliacion, 
									   Conciliacion conciliacion, int estado) throws Throwable;
	
	/**
	 * Recupera la suma de todos los movimientos activos de una cuenta bancaria en un rango de fechas y de 3 tipos de origenes distintos
	 * @param idCuenta	: Id de la cuenta bancaria
	 * @param fechaDesde: Fecha desde
	 * @param fechaHasta: Fecha hasta
	 * @param rubroOrigen1: Tipo de origen 1 tomado del rubro OrigenMovimientoConciliacion
	 * @param rubroOrigen2: Tipo de origen 2 tomado del rubro OrigenMovimientoConciliacion
	 * @param rubroOrigen3: Tipo de origen 3 tomado del rubro OrigenMovimientoConciliacion
	 * @return			: Suma de valores
	 * @throws Throwable: Excepcion
	 */
	Double selectSumValorCuentaRangoFechas3Origenes(Long idCuenta, Date fechaDesde, 
					Date fechaHasta, int rubroOrigen1, int rubroOrigen2,
					int rubroOrigen3) throws Throwable;
	
    /**
     * Metodo que recupera los registros que cumplen con la condicion de los parametros
     * 
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @param idPeriodo			: Id del periodo
	 * @param conciliacion		: Id conciliacion
	 * @param estado			: Codigo de estado
	 * @return					: Arreglo de objetos
	 * @throws Throwable		: Excepcion
	 */
	List<MovimientoBanco> selectConciliacion(Long idCuentaBancaria, Long idPeriodo, Long conciliacion, Long estado) throws Throwable;
	
	/**
	 * Recupera los movientos de una cuenta para la consulta de RIED
	 * @param idCuentaBancaria	: Id de cuenta bancaria
	 * @param fechaInicio		: Fecha inicio
	 * @param fechaFin			: Fecha fin
	 * @return					: Arreglo de objetos
	 * @throws Throwable		: Excepcion
	 */
	List<MovimientoBanco> selectRIED(Long idCuentaBancaria, Date fechaInicio, Date fechaFin) throws Throwable;
	
	/**
	 * Actualiza el tipo de movimiento y los datos de conciliacion y fecha por el id de conciliacion y el tipo de movimiento 
	 * @param idConciliacion	: Id de conciliacion
	 * @param tipoMovimiento	: Tipo de movimiento
	 * @param origenMovimiento	: Origen de movimiento
	 * @throws Throwable		: Excepcion
	 */
	void updateTipoConciliaFechaByConciliaOrigen(Long idConciliacion, int tipoMovimiento,
				int origenMovimiento) throws Throwable;
}
