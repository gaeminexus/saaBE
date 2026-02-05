package com.saa.ejb.contabilidad.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.CentroCosto;

import jakarta.ejb.Local;


@Local
public interface CentroCostoService extends EntityService<CentroCosto> {
	
	/**
	 * Método que verifica el numero de registros en una empresa
	 * @param empresa		: Id de la empresa
	 * @return 				: Numero de centros de costo en la empresa
	 * @throws Throwable	: Excepcion
	 */
	 int numeroRegistrosEmpresa(Long empresa) throws Throwable;
	
	/**
	 * Metodo que crea el nodo raiz del arbol de plan para cuando no se ingresan registros
	 * @param empresa  : Código de la empresa en la que se desea crear el nodo
	 * @return 		   : Mensaje indica si hubo errores o no.
	 */
	 String creaNodoArbolCero(Long empresa) throws Throwable ;
	
	/**
	 * Metodo para crear la cuenta a partir del numero
	 * @param object		: Arreglo de objetos que contiene los campos enviados desde el cliente
	 * @param campos		: Arreglo que contiene el nombre de los campos enviados por el cliente
	 * @return				: Mensaje
	 * @throws Throwable	: Excepcion
	 */
	 String saveCuenta(List<CentroCosto> object, Long empresa) throws Throwable;
	
	/**
	 * Metodo para verificar si la cuenta es de tipo acumulacion o movimiento
	 * @param id			: Codigo de la cuenta
	 * @return				: Tipo de cuenta 1 = ACUMULACION, 2 = MOVIMIENTO 
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
	 * Valida si es posible realiza la copia de los centros de costo
	 * @param empresaDestino: Empresa a la que se va a copiar los centros de costo
	 * @return				: True = es posible realizar copia. false = No es posible realizar la copia
	 * @throws Throwable	: Excepcion
	 */
	 boolean validaCopiaCentros(Long empresaDestino) throws Throwable;
	
	/**
	 * Copia los centros de costo de una empresa a otra
	 * @param empresaOrigen  : Empresa desde la que se va a copiar
	 * @param empresaDestino : Empresa a la que se va a copiar
	 * @throws Throwable	 : Excepcion
	 */
	 String copiaCentroEmpresa(Long empresaOrigen, Long empresaDestino) throws Throwable;
	
	/**
	 * Recupera el listado de cuentas hijo de una cuenta
	 * @param id			: Id del padre
	 * @return				: Listado de cuentas hijo
	 * @throws Throwable	: Excepcion
	 */
	 List<CentroCosto> recuperaCuentasHijo(Long id) throws Throwable;
	
	/**
	 * Cambia de estado 
	 * @param id			: Id de la cuenta a cambiar el tipo 
	 * @param tipo			: Tipo que se cambiara
	 * @throws Throwable	: Excepcion
	 */
	 void actualizaEstadoCentro(Long id, int tipo) throws Throwable;
	
	/**
	 * Busca el objeto que contiene el mayor numero de cuenta
	 * @param listadoCuenta	: Listado de las cuentas a buscar
	 * @return				: Objeto con la mayor cuenta
	 * @throws Throwable	: Excepcion
	 */
	 CentroCosto buscaMayorCuenta(List<CentroCosto> listadoCuenta) throws Throwable;
	
	/**
	 * Metodo que borra o elimina registros dependiendo de parametros
	 * 
	 * @param id			: Listado de claves de objetos a eliminar
	 * @throws Throwable	: Excepcion en caso de error
	 */
	 void remove(Long id, boolean actualiza) throws Throwable;
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	 CentroCosto selectById(Long id) throws Throwable;
	
	/**
	 * Recupera centro de costos activos de movimiento por empresa 
	 * @param empresa	: Id de empresa	
	 * @return			: Listado de registros recuperados
	 * @throws Throwable: Excepcion
	 */
	 List<CentroCosto> selectMovimientosByEmpresa(Long empresa) throws Throwable;
	 
	 /**
	 * Recupera centro de costos detalle asiento 
	 * @param empresa	: Id de empresa	
	 * @return			: Listado de registros recuperados
	 * @throws Throwable: Excepcion
	 */
	 Long validaExistenAsientos(Long idCentroCosto) throws Throwable; /* creado mely */ 
	 
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
	 * Calcula el saldo de un centro de costo a una fecha dada
	 * @param idEmpresa	: Id de la empresa
	 * @param idCentro	: Id del centro de costo
	 * @param fechaInicio: Fecha hasta la que se desea el saldo
	 * @return			: Saldo del centro a fecha
	 * @throws Throwable: Excepcion
	 */
	Double saldoCentroFechaEmpresa(Long idEmpresa, Long idCentro, LocalDate fechaInicio) throws Throwable;
	
	/**
	 * Verifica los registros asociados a un centro de costo sin tomar en cuenta los registros de mayorizacion
	 * @param id		: Id del centro de costo
	 * @return			: True = tiene registros asociados, False = no tiene registros asociados
	 * @throws Throwable: Excepcion
	 */
	boolean verificaHijosSinMayorizacin(Long id) throws Throwable;
	
	/**
	 * Verifica los registros asociados a un centro de costo sin tomar en cuenta los registros de historicos de mayorizacion
	 * @param id		: Id del centro de costo
	 * @return			: True = tiene registros asociados, False = no tiene registros asociados
	 * @throws Throwable: Excepcion
	 */
	boolean verificaHijosHistMayorizacin(Long id) throws Throwable;
	
	/**
	 * Recupera todas los centros de una empresa menos el raiz
	 * @param idEmpresa		: Id de la empresa
	 * @return				: Listado de centros hijos
	 * @throws Throwable	: Excepcion
	 */
	List<CentroCosto> selectByEmpresaSinRaiz(Long idEmpresa) throws Throwable;

}
