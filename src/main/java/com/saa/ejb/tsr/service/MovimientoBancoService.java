package com.saa.ejb.tsr.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.Asiento;
import com.saa.model.tsr.Cheque;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.Conciliacion;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.DetalleDeposito;
import com.saa.model.tsr.MovimientoBanco;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad MovimientoBanco.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface MovimientoBancoService extends EntityService<MovimientoBanco>{
 	 
	/**
	 * Metodo que Obtiene la Fecha del Primer Movimiento de una Cuenta Bancaria
	 * @param idCuenta
	 * @return
	 * @throws Throwable
	 */
	LocalDate obtieneFechaPrimerMovimiento (Long idCuenta) throws Throwable;
	
	/**
	 * Metodo para Recuperar los movimientos en transito de la cuenta bancaria en rango de fecha
	 * @param idCuenta			: Id de la cuenta para la busqueda 
	 * @param fechaInicio		: Fecha Inicial  
	 * @param fechaFin			: Fecha Finnal
	 * @return					: Movimientos
	 * @throws Throwable		: Exepciones
	 */
	Double recuperaValorTrancitoCuentaBancaria(Long idCuenta, LocalDate fechaInicio, LocalDate fechaFin)throws Throwable; 

	/**
	 * Recibir transferencias bancarias de bancos externos por proceso de cobros
	 * @param cobro		: Entidad de cobro
	 * @throws Throwable: Excepcion
	 */
	void crearMovimientoPorCobro(Cobro cobro) throws Throwable;
	
	
	/**
	 * Crea Movimiento de egreso mvbc del debito a la cuenta origen
	 * @param movimientoBanco	: Movimento banco 
	 * @return					: 
	 * @throws Throwable		: Excepcions
	 */
	void saveMovimientoBanco(Long movimientoBanco) throws Throwable;
	
	/**
	 * Registra movimiento bancario de cobro por deposito para conciliacion
	 * @param idEmpresa			: Id de la empresa
	 * @param detalleDeposito	: Entidad detalle de deposito
	 * @param asiento			: Entidad asiento
	 * @param valor				: vaValor de deposito
	 * @throws Throwable		: Excepcion
	 */
	void creaMovimientoPorDeposito(Long idEmpresa, DetalleDeposito detalleDeposito, Asiento asiento, Double valor) throws Throwable;
	
	/**
	 * Actualiza estado de movimiento dado el id del asiento
	 * @param idAsiento	: Id del asiento
	 * @param estado	: Estado
	 * @throws Throwable: Excepcion
	 */
	void actualizaEstadoMovimiento(Long idAsiento, Long estado) throws Throwable;
	
	/**
	 * Registra movimiento bancario de transferencia
	 * @param idEmpresa			: Id de la empresa
	 * @param descripcion		: Descripcion
	 * @param asiento			: Entidad asiento
	 * @param cuentaBancaria	: Entidad de cuenta bancaria
	 * @param valor				: Valor de deposito
	 * @param tipoMovimiento	: Tipo de movimiento
	 * @param origenMovimiento	: Origen de Movimiento
	 * @return					: Movimiento banco
	 * @throws Throwable		: Excepcion
	 */
	MovimientoBanco creaMovimientoPorTransferencia(Long idEmpresa, String descripcion, 
			Asiento asiento, CuentaBancaria cuentaBancaria, Double valor, 
			int tipoMovimiento, int origenMovimiento) throws Throwable;
	
	/**
	 * Registra movimiento bancario de Emision de Cheque
	 * @param idEmpresa			:Id de la empresa	  
	 * @param asiento			:Entidad asiento
	 * @param cheque			:Entidad cheque
	 * @param tipoMovimiento	:Tipo de Movimiento
	 * @param descripcion		:Descripcion
	 * @throws Throwable		:Excepcion
	 */
	void creaMovimientoPorCheque(Long idEmpresa, Asiento asiento, Cheque cheque, 
			int tipoMovimiento, String descripcion) throws Throwable;
	
	/**
	 * Recupera registros para conciliar 
	 * @param idCuentaBancaria	: Id de la Cuenta Bancaria
	 * @param fecha				: Se obtienen los registros menores a esta fecha
	 * @return					: Numero de registros encontrados
	 * @throws Throwable		: Excepcions
	 */
	 Long cuentaByCuentaBancariaEstadoMenorAFecha(Long idCuentaBancaria, LocalDate fecha) throws Throwable;
	 
	 /**
	  * Recupera los saldos de los movimientos según el banco
	 * @param idCuenta	: Id de la cuenta bancaria
	 * @param idPeriodo	: Id del periodo
	 * @return		    : Arreglo de doubles que contiene los siguientes saldos: </br>   
	 * 					  [0] = Ingresos en tránsito por depósitos</br>
	 * 					  [1] = Egresos en transito por cheques girados y no cobrados</br>
	 * 					  [2] = Notas de credito bancario en transito por notas de credito</br>
	 * 					  [3] = Notas de debito bancario en transito por notas de debito</br>
	 * 					  [4] = Ingresos en transito por transferencias en credito</br>
	 * 					  [5] = Egresos en transito por transferencias en debito
	 * @throws Throwable: Excepcion
	 */
	 Double[] saldosSegunBancos(Long idCuenta, Long idPeriodo) throws Throwable;
	 
	 /**
	 * Recupera registros para conciliar 
	 * @param idCuentaBancaria	: Id de la Cuenta Bancaria
	 * @param fecha				: Se obtienen los registros menores a esta fecha
	 * @return					: Registros que estan en estado activo y que no se encuentran en ninguna conciliacion
	 * @throws Throwable		: Excepcions
	 */
	 List<MovimientoBanco> selectSinConsByCuentaEstadoMenorAFecha
	 					(Long idCuentaBancaria, LocalDate fecha) throws Throwable;
	 
	 /**
	  * Actualiza el estado del movimiento bancario dependiendo el tipo de movimiento
	 * @param Conciliacion	: Conciliacion en la que se incluye el movimiento
	 * @param idMovimiento	: Id del movimiento bancario
	 * @param estado		: Estado original del movimiento. El proceso cambia el estado al correspondiente luego de conciliarse.
	 * @param fecha			: Fecha en la que se concilia el registro
	 * @throws Throwable	: Excepcion
	 */
	void actualizaEstadoMovimiento(Conciliacion Conciliacion, Long idMovimiento, 
			                       int estado, LocalDate fecha) throws Throwable;
	
	/**
	 * Recupera el saldo de una cuenta bancaria en un rango de fechas
	 * @param idCuenta
	 * @param fechaDesde
	 * @param fechaHasta
	 * @return
	 * @throws Throwable
	 */
	Double saldoCuentaRangoFechas(Long idCuenta, LocalDate fechaDesde, 
					LocalDate fechaHasta) throws Throwable;
	
    /**
     * Metodo que recupera los registros que cumplen con la condicion de los parametros 
     * @param campos			: Campos de la entidad
     * @param idCuentaBancaria	: Id de la cuenta bancaria 
	 * @param idPeriodo			: Id del periodo
	 * @param conciliacion		: Id conciliacion
	 * @param estado			: Codigo de estado
	 * @return					: Arreglo de objetos
	 * @throws Throwable		: Excepcion
	 */
	List<MovimientoBanco> selectConciliacion(Object[] campos, Long idCuentaBancaria, Long idPeriodo, Long conciliacion, Long estado) throws Throwable;
	
	/**
	 * Recupera los movientos de una cuenta para la consulta de RIED
	 * @param campos			: Campos de la entidad
	 * @param idCuentaBancaria	: Id de cuenta bancaria
	 * @param fechaInicio		: Fecha inicio
	 * @param fechaFin			: Fecha fin
	 * @return					: Arreglo de objetos
	 * @throws Throwable		: Excepcion
	 */
	List<MovimientoBanco> selectRIED(Object[] campos,Long idCuentaBancaria, LocalDate fechaInicio, LocalDate fechaFin) throws Throwable;
	
	/**
	 * Actualiza el tipo de movimiento, la conciliacion y la fecha de conciliacion de los registros que se desconcilian 
	 * @param idConciliacion	: Id de conciliacion
	 * @throws Throwable		: Excepcion
	 */
	void actualizaMovimientosDesconciliacion(Long idConciliacion) throws Throwable;
	
	/**
	 * Recupera Cuentas con id Periodo
	 * @param idPeriodo	: Id del Periodo
	 * @return			: Cuentas
	 * @throws Throwable: Excepcions
	 */
	List<MovimientoBanco> selectCuentasByIdPeriodo(Long idPeriodo)throws Throwable;
}
