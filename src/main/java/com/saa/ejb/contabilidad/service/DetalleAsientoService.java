package com.saa.ejb.contabilidad.service;

import java.util.Date;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.Asiento;
import com.saa.model.contabilidad.CentroCosto;
import com.saa.model.contabilidad.DetalleAsiento;
import com.saa.model.contabilidad.Mayorizacion;
import com.saa.model.contabilidad.Periodo;
import com.saa.model.contabilidad.PlanCuenta;

import jakarta.ejb.Local;

@Local
public interface DetalleAsientoService extends EntityService<DetalleAsiento> {
	
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	  DetalleAsiento selectById(Long id) throws Throwable;
	 
	/**
	 * Genera el detalle de asiento de cierre de una empresa en un periodo
	 * @param cabeceraCierre	: Asiento
	 * @param periodo			: Periodo de cierre
	 * @param mayorizacion	: Mayorizacion antes de realizar el asiento de cierre
	 * @throws Throwable		: Excepcion
	*/
	  void generaDetalleCierre(Asiento cabeceraCierre, Periodo periodo, Mayorizacion mayorizacion) throws Throwable;
	 
	 /**
	  * Graba registros con entidad
	 * @param detalleAsiento	: Entidad a persistir
	 * @param codigo			: Id de la entidad a persistir
	 * @throws Throwable		: Excepcion
	 */
	 void save(DetalleAsiento detalleAsiento, Long codigo) throws Throwable;
	
	/**
	 * Genera el detalle de asiento de reversion
	 * @param asientoOriginal	: Asiento a reversar
	 * @param asientoReversion	: Asiento de reversion
	 * @throws Throwable		: Excepcion
	 */
	 void generaDetalleReversion(Asiento asientoOriginal, Asiento asientoReversion) throws Throwable;

	/**
	 * Almacena el detalle de asiento contable y devuelve id
	 * @param detalleAsiento: Entidad Detalle asiento
	 * @return				: Id de detalle de asiento
	 * @throws Throwable	: Excepcion
	 */
	 Long saveDetalle(DetalleAsiento detalleAsiento) throws Throwable;
	
	/**
	 * Valida que el debe sea igual al haber en un asiento contable
	 * @param idAsiento	: Id del asiento
	 * @return			: Variable si indica si el debe es igual al haber
	 * @throws Throwable: Excepcion
	 */
	 boolean validaDebeHaber(Long idAsiento) throws Throwable;
	
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
	 * Inserta el detalle del asiento del debe
	 * @param planCuenta	: Entidad PlanCuentas
	 * @param descripcion	: Descripcion del detalle de asiento
	 * @param valor			: Valor del detalle de asiento
	 * @param asiento		: Entidad asiento
	 * @param cetroCosto	: Entidad cetro de costo
	 * @throws Throwable	: Excepcion
	 */
	 void insertarDetalleAsientoDebe(PlanCuenta planCuenta, String descripcion, Double valor, Asiento asiento, CentroCosto centroCosto) throws Throwable;
	
	/**
	 * Inserta el detalle del asiento del haber
	 * @param planCuenta	: Entidad PlanCuentas
	 * @param descripcion	: Descripcion del detalle de asiento
	 * @param valor			: Valor del detalle de asiento
	 * @param asiento		: Entidad asiento
	 * @param cetroCosto	: Entidad cetro de costo
	 * @throws Throwable	: Excepcion
	 */
	 void insertarDetalleAsientoHaber(PlanCuenta planCuenta, String descripcion, Double valor, Asiento asiento, CentroCosto centroCosto) throws Throwable;
	
	/**
	 * Valida que el Debe sea igual al Haber en un Asiento Contable
	 * @param asiento	: AsientoCodigo
	 * @throws Throwable: Excepcions
	 */
	boolean validaDebeHaberAsientoContable (Asiento asiento)throws Throwable;
	
	/**
	 * Obtiene el Debe y Haber de las Cuentas Contables
	 * @param empresa			: Id de la Empresa
	 * @param fechaInicio		: Fecha Inicio
	 * @param fecaFin			: Fecha Fin
	 * @param idCuentaContable	: Id de la Cuenta Contable
	 * @return					: Debe y Haber
	 * @throws Throwable		: Excepcions
	 */
	Double[] selectSumaDebeHaberByFechasEmpresaCuenta(Long empresa, Date fechaInicio, Date fechaFin, Long idCuentaContable )throws Throwable;
	
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
	List<DetalleAsiento> selectMovimientoByEmpresaCuentaFecha(Long empresa, Date fechaInicio, Date fechaFin, String cuentaInicio, String cuentaFin)throws Throwable;
	
	/**
	 * Recupera los registros de una empresa que se encuentren entre las fechas indicadas y que pertenezcan a una cuenta 
	 * @param empresa	: Id empresa
	 * @param idCuenta	: Id de la Cuenta Contable
	 * @return			: DetalleAsiento
	 * @throws Throwable: Excepcions
	 */
	List<DetalleAsiento> selectByEmpresaCuentaFechas(Long empresa, Long idCuenta, Date fechaInicio, Date fechaFin) throws Throwable;
	
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
	List<DetalleAsiento> selectByEmpresaCuentaFechaCentro(Long empresa, Date fechaInicio, Date fechaFin, 
			String cuentaInicio, String cuentaFin, String centroInicio,
			String centroFin) throws Throwable;
	
	/**
	 * Recupera el saldo de una cuenta en una empresa dado un rango de fechas
	 * @param idEmpresa	: Id de la empresa en la que se realiza la búsqueda
	 * @param idCuenta	: Id de la cuenta sobre la que se obtiene el saldo
	 * @param fechaDesde: Fecha desde la que se realiza la búsqueda
	 * @param fechaFin	: Fecha hasta la que se realiza la búsqueda
	 * @return
	 */
	Double recuperaSaldoCuentaEmpresaFechas(Long idEmpresa, Long idCuenta, Date fechaDesde, Date fechaFin) throws Throwable;
	
	/**
	 * Obtiene los Movimientos de una Empresa dado un rango de fechas, cuentas, y centro de costo
	 * @param idCuenta		: Id de la cuenta contable
	 * @param fechaInicio	: Fecha Inicio 
	 * @param fechaFin		: Fecha Fin
	 * @param centroInicio	: Centro de costo Inicio 
	 * @param centroFin		: Centro de costo Fin 
	 * @return				: Movimientos
	 * @throws Throwable	: Excepcions
	 */
	List<DetalleAsiento> selectByCuentaFechasCentros(Long idCuenta, Date fechaInicio, Date fechaFin, 
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
	List<DetalleAsiento> selectByCentroFechasCuentas(Long idCentro, Date fechaInicio, Date fechaFin, 
			String cuentaInicio, String cuentaFin) throws Throwable;
	
	/**
	 * Recupera el saldo de un centro de costo en una empresa dado un rango de fechas
	 * @param idEmpresa	: Id de la empresa en la que se realiza la búsqueda
	 * @param idCentro	: Id del centro de costo sobre el que se obtiene el saldo
	 * @param fechaDesde: Fecha desde la que se realiza la búsqueda
	 * @param fechaFin	: Fecha hasta la que se realiza la búsqueda
	 * @return
	 */
	Double recuperaSaldoCentroEmpresaFechas(Long idEmpresa, Long idCentro, Date fechaDesde, Date fechaFin) throws Throwable;
	
	/**
	 * Obtiene el Debe y Haber de las Cuentas Contables filtradas por cuenta, empresa y centro de costo
	 * @param empresa			: Id de la Empresa
	 * @param fechaInicio		: Fecha Inicio
	 * @param fecaFin			: Fecha Fin
	 * @param idCuentaContable	: Id de la Cuenta Contable
	 * @param idCentro			: Id del centro de costo
	 * @return					: Debe y Haber
	 * @throws Throwable		: Excepcions
	 */
	Double[] selectSumaDebeHaberByFechasEmpresaCentroCuenta
			(Long empresa, Date fechaInicio, Date fechaFin, Long idCuentaContable, Long idCentro)throws Throwable;
	
	/**
	 * Recupera la suma del debe y el haber de una cuenta en un centro hasta una fecha dada
	 * @param idEmpresa	: Id de la empresa en la que se realiza
	 * @param idCentro	: Id del centro de costo del que se desea obtener el saldo
	 * @param idCuenta	: Id de la cuenta contable de la que se desea obtener el saldo
	 * @param fechaHasta: Fecha hasta la que se realiza la suma
	 * @return			: Suma total del debe y haber
	 * @throws Throwable: Excepcion
	 */
	Double[] selectSumaDebeHaberAFechaByEmpresaCentroCuenta(Long idEmpresa, 
			Long idCentro, Long idCuenta, Date fechaHasta) throws Throwable;	
	
	/**
	 * Recupera el saldo de un centro de costo en una cuenta contable en una empresa hasta una fecha
	 * @param idEmpresa	: Id de la empresa en la que se realiza la búsqueda
	 * @param idCentro	: Id del centro de costo sobre el que se obtiene el saldo
	 * @param idCuenta	: Id de la cuenta contable de la que se desea obtener el saldo
	 * @param fechaHasta	: Fecha hasta la que se realiza la búsqueda
	 * @return
	 */
	Double recuperaSaldoCuentaCentroEmpresaAFecha(Long idEmpresa, 
			Long idCentro, Long idCuenta, Date fechaHasta) throws Throwable;	
}
