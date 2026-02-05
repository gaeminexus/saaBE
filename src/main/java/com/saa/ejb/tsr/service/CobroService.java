package com.saa.ejb.tsr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.tsr.CierreCaja;
import com.saa.model.tsr.Cobro;
import com.saa.model.tsr.TempCobro;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Cobro.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface CobroService extends EntityService<Cobro>{

 	 
	/**	
	 * Almacena un cobro ingresado
	 * @param idTempCobro	: Id de temporal de cobro
	 * @return			 	: Arreglo con el id de asiento, numero de asiento, id de cobro y mensaje de ingreso
	 * @throws Throwable 	: Excepcion
	 */
	String[] crearCobrosIngresados(Long idTempCobro) throws Throwable;
	 
	/**
	 * Copia el cobro temporal a un cobro real
	 * @param idTempCobro	: Id de cobro a copiar
	 * @return			 	: Cobro ingresado
	 * @throws Throwable 	: Excepcion
	 */
	Cobro copiarCobrosTemporalesAReales(Long idTempCobro) throws Throwable;
	 
	/**
	 * Almacena el cobro real
	 * @param tempCobro	: Entidad temporal de cobro
	 * @throws Throwable	: Excepcion
	 */
	Cobro saveCobroReal(TempCobro tempCobro) throws Throwable;
	 
	/**
	 * Crea el asiento de un cobro
	 * @param cobro		: Entidad cobro
	 * @return			: Vector con id de asiento y numero de asiento
	 * @throws Throwable: Excepcion
	 */
	Long[] crearAsientoCobro(Cobro cobro) throws Throwable;
	
	/**
	 * Crea el asiento de un cobro
	 * @param cobro		: Entidad cobro
	 * @return			: Vector con id de asiento y numero de asiento
	 * @throws Throwable: Excepcion
	 */
	Long[] crearAsientoClienteMotivo(Cobro cobro) throws Throwable;
	
	/**
	 * Inserta el detalle del debe en el asiento
	 * @param cobro		: Entidad cobro 
	 * @param idAsiento	: Id del asiento
	 * @throws Throwable: Excepcion
	 */
	void insertaDetalleDebe(Cobro cobro, Long idAsiento) throws Throwable;
	
	/**
	 * Inserta el detalle del haber de cuentas por cobrar cliente en el asiento
	 * @param cobro			: Cobro realizado
	 * @param idAsiento		: Id del asiento
	 * @param valor			: valor a registrar
	 * @throws Throwable	: Excepcion
	 */
	void insertaDetalleHaberCxC(Cobro cobro, Long idAsiento, Double valor) throws Throwable;
	
	/**
	 * Inserta el detalle del debe de cuentas por cobrar cliente en el asiento
	 * @param cobro		: Cobro realizado
	 * @param idAsiento	: Id del asiento
	 * @param valor		: valor a registrar
	 * @throws Throwable: Excepcion
	 */
	void insertaDetalleDebeCxC(Cobro cobro, Long idAsiento, Double valor) throws Throwable;
	
	/**
	 * Inserta el detalle del haber de motivos de cobros en el asiento
	 * @param cobro		: Entidad cobro
	 * @param idAsiento	: Id del asiento
	 * @throws Throwable: Excepcion
	 */
	void insertaDetalleHaberMotivo(Cobro cobro, Long idAsiento) throws Throwable;
	
	
	/**
	 * Inserta el detalle del asiento con efectivo y cheque
	 * @param cobro		: Cobro realizado
	 * @param valor		: Valor a asignar
	 * @param idAsiento	: Id del Asiento
	 * @throws Throwable: Excepcion
	 */
	void insertarDetalleEfectivo(Cobro cobro, Double valor, Long idAsiento) throws Throwable;
	
	/**
	 * Inserta el detalle del asiento con tarjeta de credito
	 * @param cobro		: Cobro realizado
	 * @param idAsiento	: Id del asiento
	 * @throws Throwable: Excepcion
	 */
	void insertarDetalleTarjeta(Cobro cobro, Long idAsiento) throws Throwable;
	
	/**
	 * Inserta el detalle del asiento con transferencia
	 * @param cobro		: Cobro realizado
	 * @param idAsiento	: Id del asiento
	 * @throws Throwable: Excepcion
	 */
	void insertarDetalleTransferencia(Cobro cobro, Long idAsiento) throws Throwable;
	
	/**
	 * Inserta el detalle del asiento con retención
	 * @param cobro		: Cobro realizado
	 * @param idAsiento	: Id del asiento
	 * @throws Throwable: Excepcion
	 */
	void insertarDetalleRetencion(Cobro cobro, Long idAsiento) throws Throwable;
	
	/**
	 * Obtiene la cuenta contable de cliente dado un cobro
	 * @param idCobro	: Id de cobro
	 * @param empresa	: Id de empresa
	 * @return			: cuenta contable recuperada
	 * @throws Throwable: Excepcion
	 */
	PlanCuenta obtenerCuentaBancariasCliente(Long idCobro, Long empresa) throws Throwable;
	
	/**
	 * Actualiza el asiento contable en la tabla de cobros
	 * @param cobro			: Entidad cobro
	 * @param idAsiento		: Id de asiento
	 * @param numeroAsiento	: Numero del asiento
	 * @throws Throwable	: Excepcion
	 */
	void actualizarAsientoCobro(Cobro cobro, Long idAsiento, Long numeroAsiento) throws Throwable;
	
	/**
	 * Recupera los cobro de un cierre de caja
	 * @param idCierreCaja	: Id de cierre de caja
	 * @return				: Lista de cobro
	 * @throws Throwable	: Excepcion
	 */
	List<Cobro> selectByCierreCaja(Long idCierreCaja) throws Throwable;
	
	/**
	 * Metodo que valida los cobros pendientes
	 * @param idUsuario	: Id del Usuario 
	 * @throws Throwable: Excepcions
	 */
	void validaCobrosPendientes (Long idUsuario) throws Throwable;
	 
	/**
	 * Valida si se puede anulacion del cobro o no
	 * @param idCobro	: Id del cobro
	 * @throws Throwable: Excepcion
	 */
	void validaAnulacionCobro (Long idCobro) throws Throwable;
	
	/**
	 * Anula un cobro ingresado
	 * @param idCobro			: Id de cobro
	 * @param motivoAnulacion	: Codigo de motivo de anulacion
	 * @throws Throwable		: Excepcion
	 */
	void anulaCobroIngresado(Long idCobro, int motivoAnulacion) throws Throwable;
	
	/**
	 * Metodo para anular cobro
	 * @param cobro				: Entidad cobro
	 * @param motivoAnulacion	: Codigo de motivo de anulacion
	 * @throws Throwable		: Excepcion
	 */
	void anulaCobro(Cobro cobro, int motivoAnulacion) throws Throwable;
	 
	/**
	 * Actualiza el estado del cobro
	 * @param cobro			: Entidad cobro a actualizar
	 * @param estadoCobro	: Estado de cobro por q se va a actualizar
	 * @throws Throwable	: Excepcion
	 */
	void actualizaEstadoCobro(Cobro cobro, int estadoCobro)throws Throwable;
	
	/**
	 * Actualiza el estado del cobro a cerrado y añade cierre de caja
	 * @param cierreCaja: Cierre de caja
	 * @param cobro		: Cobro 
	 * @throws Throwable: Excepcion	
	 */
	void actualizaCobroCerrado(CierreCaja cierreCaja, Cobro cobro)throws Throwable;
	
	/**
	 * Recupera los cobros para cierre de caja
	 * @param idUsuarioCaja	: Id de usuario por caja
	 * @return				: Elementos recuperados
	 * @throws Throwable	: Excepcion
	 */
	List<Cobro> selectVistaByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Recupera los cobros para cierre de caja
	 * @param idUsuarioCaja	: Id de usuario por caja
	 * @return				: Elementos recuperados
	 * @throws Throwable	: Excepcion
	 */
	List<Cobro> selectCobroByUsuarioCaja(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Recupera Cobros asociados a un Cierre
	 * @param cierreCaja	: Cierre de Caja
	 * @return				: Listado Cobro
	 * @throws Throwable	: Excepcions
	 */
	List<Cobro> selectCobroByCierreCaja (Long cierreCaja)throws Throwable;
	
}
