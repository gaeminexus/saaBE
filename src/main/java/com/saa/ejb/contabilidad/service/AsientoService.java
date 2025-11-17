package com.saa.ejb.contabilidad.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.Asiento;
import com.saa.model.contabilidad.Mayorizacion;
import com.saa.model.contabilidad.Periodo;
import com.saa.model.contabilidad.PlanCuenta;
import com.saa.model.scp.DetalleRubro;

import jakarta.ejb.Remote;

@Remote
public interface AsientoService extends EntityService<Asiento> {
	
	/**
	 * Variable para desplegar mensajes
	 */
	
	 /**
	 * Recupera entidad con el id
	 * @param id		: Id de la entidad
	 * @return			: Recupera entidad
	 * @throws Throwable: Excepcion
	 */
	 Asiento selectById(Long id) throws Throwable;
	
	/**
	 * Recupera los anios ingresados en los asientos de una empresa
	 * @param empresa	: Id de la empresa en la que se realiza la busqueda
	 * @return			: Arreglo de objetos con los anios
	 * @throws Throwable: Excepcion
	 */
	 List<Long> selectAniosAsiento(Long empresa) throws Throwable;
	
	/**
	 * Recupera los usuarios que han  ingresados asientos de una empresa
	 * @param empresa	: Id de la empresa en la que se realiza la busqueda
	 * @return			: Arreglo de objetos con los usuarios
	 * @throws Throwable: Excepcion
	 */
	 List<String> selectUsuarioAsiento (Long empresa)throws Throwable;
	
	/**
	 * Recupera el listado de todos los asientos que han sido usados para reversar otros
	 * @param empresa	: Id de la empresa en la que se realiza la búsqueda
	 * @return			: Listado con resultado de select
	 * @throws Throwable: Excepcion
	 */
	 List<Asiento> selectAsientoReverso(Long empresa) throws Throwable;
	
	/**
	 * Recupera los módulos que el usuario no ha adquirido
	 * @return			: Arreglo con listado de modulos
	 * @throws Throwable: Excepcion
	 */
	 List<DetalleRubro> comboModulos() throws Throwable;
	
	/**
	 * Actualiza los asientos activos o reversado de un periodo a mayorizados 
	 * @param periodo		: Periodo
	 * @param mayorizacion	: Mayorizacin
	 * @throws Throwable	: Excepcion
	 */
	 void actualizaMayorizacionByPeriodo(Periodo periodo, Mayorizacion mayorizacion) throws Throwable;
	
	/**
	 * Recupera el siguiente numero de asiento de un tipo en una empresa
	 * @param tipo		: Tipo de asiento contable
	 * @param empresa	: Id de empresa
	 * @return			: Numero de asiento
	 * @throws Throwable: Excepcion
	 */
	 Long siguienteNumeroAsiento(Long tipo, Long empresa) throws Throwable;
	
	/**
	 * Genera la cabecera del asiento de cierre para un período
	 * @param periodo	: Periodo en el que se genera el asiento de cierre
	 * @param empresa	: Id de la empresa
	 * @param usuario	: Usuario que realiza el asiento de cierre
	 * @throws Throwable: Excepcion
	 */
	 void generaCabeceraCierre(Periodo periodo, Long empresa, String usuario) throws Throwable;
	
	/**
	 * Genera el asiento de cierre para un periodo
	 * @param periodo	: Periodo en el que se genera el asiento de cierre
	 * @param empresa	: Empresa en la que se realiza el cierre
	 * @param usuario	: Usuario que realiza el cierre
	 * @param mayorizacion	: Mayorizacion antes de realizar el asiento de cierre
	 * @throws Throwable: Excepcion
	 */
	 void generaAsientoCierre(Long periodo, Long empresa, String usuario, Mayorizacion mayorizacion) throws Throwable;
	
	/**
	 * Almacena cabecera de asiento contable y devuelve id generado
	 * @param object	: Arreglo de datos
	 * @param campos	: Arreglo de nombre de campos
	 * @return			: Id de objeto generado
	 * @throws Throwable:Excepcion
	 */
	 Long saveCabecera(List<Asiento> object) throws Throwable;
	
	/**
	 * Almacena cabecera de asiento contable y devuelve id generado
	 * @param object	: Arreglo de datos
	 * @param campos	: Arreglo de nombre de campos
	 * @return			: Id de objeto generado
	 * @throws Throwable:Excepcion
	 */
	/**
	 * Almacena cabecera de asiento contable y devuelve id  y numero generado
	 * @param asiento	: Entidad Asiento
	 * @return			: Id del asiento y numero de asiento
	 * @throws Throwable: Excepcion
	 */
	 Long[] saveCabecera(Asiento asiento) throws Throwable;
	
	/**
	 * Recupera un asiento por numero, empresa y tipo
	 * @param numero	: Numero de asiento
	 * @param empresa	: Id de la empresa
	 * @param tipo		: Id de tipo de asiento
	 * @return			: Asiento recuperado
	 * @throws Throwable: Excepcion
	 */
	 Asiento selectByNumeroEmpresaTipo(Long numero, Long empresa, Long tipo) throws Throwable;

	/**
	 * Revesa los datos de un Asiento 
	 * @param idAsiento	: Id Asiento
	 * @throws Throwable: Excepcion
	 */
	 void reversionAsiento(Long idAsiento) throws Throwable;
	
	/**
	 * Verifica si se puede Anular o Reversar Asiento
	 * @param asiento	: Asiento
	 * @param proceso	: Indica Proceso a Realizar Tomado del Rubro ProcesosAsiento
	 * @return			: True = Permite Proceso, False = No Permite Proceso
	 * @throws Throwable: Excepcion
	 */
	 boolean verificaAnulacionReversion (Asiento asiento, int proceso) throws Throwable;
	
	/**
	 * Genera cabecera de asiento de reversion
	 * @param asientoOriginal	: Asiento original
	 * @return					: Asiento de reversion
	 * @throws Throwable		: Excepcion
	 */
	 Asiento generaCabeceraReversion(Asiento asientoOriginal) throws Throwable;
	
	/**
	 * Recupera por codigo de mayorizacion
	 * @param idMayorizacion	: Id de mayorizacion
	 * @return					: Registros de asiento
	 * @throws Throwable		: Excepcion
	 */
	 List<Asiento> selectByMayorizacion(Long idMayorizacion) throws Throwable;
	
	/**
	 * Recupera los datos para insertar un asiento
	 * @param alternoTipo	: Codigo alterno del tipo de asiento
	 * @param empresa		: Id de la empresa
	 * @return				: Recupera datos de asiento
	 * @throws Throwable	: Excepcion
	 */
	 Asiento recuperaDatosAsiento(int alternoTipo, Long empresa) throws Throwable;
	
	
	/**
	 * Inserta la cabecera del asiento de un cobro
	 * @param empresa		: Id de la empresa
	 * @param nombreUsuario : Nombre del usuario que crea el cobro 
	 * @param observacion	: Observaciones
	 * @param alternoTipo	: codigo alterno del tipo de asiento
	 * @return				: Vector con id asiento y numero de asiento
	 * @throws Throwable	: Excepcion
	 */
	 Long[] insertarCabeceraAsiento(Long empresa, String nombreUsuario, String observacion, int alternoTipo) throws Throwable;
		
	/**
	 * Anula el asiento de cierre de un periodo
	 * @param empresa	: Id de la empresa en la que se genero el asiento
	 * @param periodo	: Id de periodo
	 * @throws Throwable: Excepcion
	 */
	 void anulaAsientoCierre(Long empresa, Long periodo) throws Throwable;
	
	/**
	 * Anula un asiento contable por el id
	 * @param idAsiento	: Id del asiento a anular
	 * @throws Throwable: Excepcion
	 */
	 void anulaAsiento(Long idAsiento) throws Throwable;
	
	
	/**
	 * Genera Cabecera de asiento de Transferencia
	 * @param empresa	: Id de la Empresa 
	 * @param usuario	: Usuario que crea el asiento 
	 * @param concepto	: Observaciones
	 * @return			: Codigo de Asiento y Numero de Asiento
	 * @throws Throwable: Excepcions
	 */
	 Asiento generaCabeceraTransferencia (Long empresa, String usuario, String concepto) throws Throwable;	
	
	/**
	 * Inserta el cobro en el detalle del asiento del debe
	 * @param planCuenta	: Entidad PlanCuentas
	 * @param descripcion	: Descripcion del detalle de asiento
	 * @param valor			: Valor del detalle de asiento
	 * @param idAsiento		: Id de asiento
	 * @throws Throwable	: Excepcion
	 */
	 void insertarCobroDetalleAsientoDebe(PlanCuenta planCuenta, String descripcion, Double valor, Long idAsiento) throws Throwable;
	 
	 /**
	  * Elimina la mayorizacion relacionada con el asiento contable
	 * @param idMayorizacion	: Id de mayorizacion
	 * @throws Throwable		: Excepcion
	 */
	void eliminaIdMayorizacion(Long idMayorizacion) throws Throwable;
	
	
}
