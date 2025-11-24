package com.saa.ejb.contabilidad.service;

import java.util.Date;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.PlanCuenta;

import jakarta.ejb.Local;

@Local
public interface PlanCuentaService extends EntityService<PlanCuenta> {
 	
	/**
	 * Constante que indica el grupo de cuentas de activo
	 */
	 int GRUPO_ACTIVO = 1;
	
	/**
	 * Método que verifica el numero de registros en una empresa
	 * @param empresa		: Id de la empresa
	 * @return numero de cuentas contables en la empresa
	 * @throws Throwable	: excepcion
	 */
	 int numeroRegistrosEmpresa(Long empresa) throws Throwable;
	
	/**
	 * Metodo que crea el nodo raiz del arbol de plan para cuando no se ingresan registros
	 * @param empresa  Codigo de la empresa en la que se desea crear el nodo
	 * @return Mensaje que indica si hubo errores o no.
	 */
	 String creaNodoArbolCero(Long empresa) throws Throwable;
	
	/**
	 * Metodo para crear la cuenta contable a partir del numero de cuenta
	 * @param object		: Arreglo de objetos que contiene los campos enviados desde el cliente
	 * @param campos		: Arreglo que contiene el nombre de los campos enviados por el cliente
	 * @return				: Mensaje
	 * @throws Throwable	: Excepcion
	 */
	 String saveCuenta(List<PlanCuenta> object, Long empresa) throws Throwable;	
	
	/**
	 * Metodo para verificar si la cuenta es de tipo acumulacion o movimiento
	 * @param id			:Codigo de la cuenta
	 * @return				:Tipo de cuenta 1 = ACUMULACION, 2 = MOVIMIENTO 
	 * @throws Throwable	: Excepcion
	 */
	 Long recuperaTipo(Long id) throws Throwable;
	
	/**
	 * Elimina una cuenta contable de movimiento
	 * @param id			: Id de la cuenta a eliminar
	 * @throws Throwable	: Excepcion
	 */
	 void removeMovimiento(Long id) throws Throwable; 
	
	/**
	 * Elimina una cuenta contable de acumulacion
	 * @param id			: Id de la cuenta a eliminar
	 * @param actualiza		: Indica si debe actualiza o eliminar. true = actualiza.
	 * @throws Throwable	: Excepcion
	 */
	 void removeAcumulacion(Long id, boolean actualiza) throws Throwable;
	
	/**
	 * Metodo que recupera el siguiente numero de cuenta a ingresar
	 * @param id			: Id de la cuenta en la que se va a insertar
	 * @return				: String de la siguiente cuenta hija
	 * @throws Throwable	: Excepcion
	 */
	 String recuperaSiguienteHijo(Long id) throws Throwable;	
	
	/**
	 * Metodo que recupera el codigo de padre de la cuenta
	 * @param id			: Id de la cuenta a buscar el padre
	 * @return				: Id del padre
	 * @throws Throwable	: Excepcion
	 */
	 Long recuperaIdPadre(Long id) throws Throwable;
	
	/**
	 * Cambia de tipo la cuenta contable
	 * @param id			: Id de la cuenta a cambiar el tipo 
	 * @param tipo			: Tipo que se cambiara
	 * @throws Throwable	: Excepcion
	 */
	 void actualizaTipoCuenta(Long id, int tipo) throws Throwable;
	
	/**
	 * Valida si es posible realiza la copia del plan
	 * @param empresaDestino	: empresa a la que se va a copiar el plan de cuentas
	 * @return					: true = es posible realizar copia. false = No es posible realizar la copia
	 * @throws Throwable		: Excepcion
	 */
	 boolean validaCopiaPlan(Long empresaDestino) throws Throwable;
	
	/**
	 * Copia el plan de cuenta de una empresa a otra
	 * @param empresaOrigen  	:Empresa desde la que se va a copiar
	 * @param empresaDestino 	:Empresa a la que se va a copiar
	 * @throws Throwable	 	: Excepcion
	 */
	 String copiaPlanEmpresa(Long empresaOrigen, Long empresaDestino) throws Throwable;
	
	/**
	 * Recupera el listado de cuentas hijo de una cuenta
	 * @param id			: Id del padre
	 * @return				: Listado de cuetnas hijo
	 * @throws Throwable	: Excepcion
	 */
	 List<PlanCuenta> recuperaCuentasHijo(Long id) throws Throwable;
	
	/**
	 * Cambia de estado la cuenta contable
	 * @param id			: Id de la cuenta a cambiar el tipo 
	 * @param tipo			: Tipo que se cambiara
	 * @throws Throwable	: Excepcion
	 */
	 void actualizaEstadoCuenta(Long id, int tipo) throws Throwable;
	
	/**
	 * Busca el objeto que contiene el mayor numero de cuenta
	 * @param listadoCuenta	: Listado de las cuentas a buscar
	 * @return				: Objeto con la mayor cuenta
	 * @throws Throwable	: Excepcion
	 */
	 PlanCuenta buscaMayorCuenta(List<PlanCuenta> listadoCuenta) throws Throwable;
	
	/**
	 * Metodo que borra o elimina registros dependiendo de parametros
	 * 
	 * @param id			: Listado de claves de objetos a eliminar
	 * @throws Throwable	: Excepcion en caso de error
	 */
	 void remove(Long id, boolean actualiza) throws Throwable;
	
	/**
	 * Valida que existan cuentas activas para las naturalezas basicas
	 * @param empresa		: Id de empresa en la que se valida
	 * @return				: Mensaje de error
	 * @throws Throwable	: Excepcion
	 */
	 String validaExisteCuentasNaturaleza(Long empresa) throws Throwable;
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	 PlanCuenta selectById(Long id) throws Throwable;
	
	/**
	 * Recupera las cuentas de una empresa que manejan centro de costo
	 * @param empresa	: Id de la empresa
	 * @return			: Listado de cuentas que cumplen la condicion
	 * @throws Throwable: Excepcion
	 */
	 List<PlanCuenta> selectByEmpresaManejaCC(Long empresa) throws Throwable;
	 
	/**
	 * Obtiene la maxima de una cuenta en una empresa dado su naturaleza dado una cuenta contable
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Contable
	 * @param siguienteNaturaleza: Siguiente naturaleza de cuenta
	 * @return				: Maxima Cuenta
	 * @throws Throwable	: Excepcions
	 */
	String recuperaMaximaCuenta (Long empresa, String cuentaInicio, String siguienteNaturaleza)throws Throwable;
	
	/**
	 * Recupera por el numero de cuenta y el id de empresa
	 * @param cuenta 	: Cuenta
	 * @param empresa	: Id de la Empresa
	 * @return			: Numero de Cuenta
	 * @throws Throwable: Excepcions
	 */
	PlanCuenta selectByCuentaEmpresa(String cuenta, Long empresa)throws Throwable;
	
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
	 * Calcula el saldo de una cuenta contable a una fecha dada
	 * @param idEmpresa	: Id de la empresa
	 * @param idCuenta	: Id de la cuenta contable
	 * @param fechaInicio: Fecha hasta la que se desea el saldo
	 * @return			: Saldo de cuenta a fecha
	 * @throws Throwable: Excepcion
	 */
	Double saldoCuentaFechaEmpresa(Long idEmpresa, Long idCuenta, Date fechaInicio) throws Throwable;
	
	/**
	 * Verifica los registros asociados a una cuenta contable sin tomar en cuenta los registros de mayorizacion
	 * @param id		: Id de la cuenta contable
	 * @return			: True = tiene registros asociados, False = no tiene registros asociados
	 * @throws Throwable: Excepcion
	 */
	boolean verificaHijosSinMayorizacin(Long id) throws Throwable;
	
	/**
	 * Verifica los registros asociados a una cuenta contable sin tomar en cuenta los registros de HISTORICOS de mayorizacion
	 * @param id		: Id de la cuenta contable
	 * @return			: True = tiene registros asociados, False = no tiene registros asociados
	 * @throws Throwable: Excepcion
	 */
	boolean verificaHijosSinHistMayorizacin(Long id) throws Throwable;
	
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
	 * Recupera cuentas en un rango por empresa
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio
	 * @param cuentaFin		: CuentaFin
	 * @return				: Rango de Cuentas
	 * @throws Throwable	: Excepcions
	 */
	List<PlanCuenta> recuperaCuentasByRango(Long empresa, String cuentaInicio, String cuentaFin)throws Throwable;
	
	/**
	 * Muestra en verdadero o falso en el rango de cuentas a comparar
	 * @param empresa		: Id de la Empresa
	 * @param cuentaInicio	: Cuenta Inicio 
	 * @param cuentaFin		: CuentaFin 
	 * @param cuentaComparar: Cuenta a Comparar
	 * @return				: Verdadero o Falso 
	 * @throws Throwable	:  Excepcions
	 */
	boolean servicioRango (Long empresa, String cuentaInicio, String cuentaFin, String cuentaComparar )throws Throwable;
}
