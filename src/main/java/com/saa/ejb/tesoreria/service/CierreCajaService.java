package com.saa.ejb.tesoreria.service;

import java.util.List;
import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.CierreCaja;
import com.saa.model.tesoreria.Deposito;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CierreCaja.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface CierreCajaService extends EntityService<CierreCaja>{
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	CierreCaja selectById(Long id) throws Throwable;
	
	/**
	 * Actualiza el estado de cierre de caja para un deposito
	 * @param cierreCaja	: Cierre de caja
	 * @param deposito		: Deposito a actualizar
	 * @param estado		: valor del estado a actualizar
	 * @throws Throwable	: Excepcion
	 */
	void actualizaEstadoCierres(CierreCaja cierreCaja, Deposito deposito, Long estado) throws Throwable;
	 
	/**
	 * Abre una caja cerrada
	 * @param idCierre		: Id del cierre de caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @throws Throwable	: Excepcion
	 */
	void abreCierreCaja(Long idCierre, Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Valida reapertura de una caja cerrada
	 * @param idCierre		: Id del cierre de caja
	 * @param idUsuarioCaja	: Id de usuario caja
	 * @throws Throwable	: Excepcion
	 */
	void validaReaperturaCaja(Long idCierre, Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Genera un cierre de caja
	 * @param idEmpresa		: Id de la empresa
	 * @param idUsuarioCaja : Id del usuario por caja
	 * @param idCajaFisica	: Id de caja fisica
	 * @param nombreUsuario	: Nombre del usuario que cierra la caja
	 * @param valoresIngreso: Valores de ingreso
	 * @return				: Id de cierre de caja
	 * @throws Throwable	: Excepcion
	 */
	Long[] generaCierreCaja(Long idEmpresa, Long idUsuarioCaja, Long idCajaFisica, String nombreUsuario, Double[] valoresIngreso) throws Throwable;
	
	/**
	 * Inserta un cierre de caja
	 * @param idUsuarioCaja : Id del usuario por caja
	 * @param nombreUsuario	: Nombre del usuario que cierra la caja
	 * @param valoresIngreso: Valores de ingreso
	 * @return				: Id de cierre de caja
	 * @throws Throwable	: Excepcion
	 */
	Long insertaCierreCaja(Long idUsuarioCaja, String nombreUsuario, Double[] valoresIngreso) throws Throwable;
	
	/**
	 * Crea el asiento de cierre de caja
	 * @param idEmpresa		: Id de empresa
	 * @param idCierreCaja	: Id de cierre de caja
	 * @param idCajaFisica	: Id de caja fisica
	 * @param idUsuarioCaja : Id del usuario por caja
	 * @param nombreUsuario	: Nombre del usuario
	 * @param valor			: Valor a depositar
	 * @return				: Vector con id de asiento y numero de asiento
	 * @throws Throwable	: Excepcion
	 */
	Long[] crearAsientoCierre(Long idEmpresa, Long idCierreCaja, Long idCajaFisica, Long idUsuarioCaja, String nombreUsuario, Double valor) throws Throwable;
	
	/**
	 * Inserta el detalle del debe del asiento de cierre de caja
	 * @param idCajaFisica	: Id de caja fisica
	 * @param idAsiento		: Id de asiento de cierre
	 * @param valor			: Valor a depositar
	 * @throws Throwable	: Escepcion
	 */
	void insertaDetalleDebe(Long idCajaFisica, Long idAsiento, Double valor) throws Throwable;
	
	/**
	 * Inserta el detalle del haber del asiento de cierre de caja
	 * @param idUsuarioCaja	: Id de cierre de caja
	 * @param idAsiento		: Id de asiento de cierre 
	 * @throws Throwable	: Excepcion
	 */
	void insertaDetalleHaber(Long idCierreCaja, Long idAsiento) throws Throwable;
	
	/**
	 * Recuprera Listado de Cierre Caja por idUsuario, Deposito, RubroEstadoH  
	 * @param idDeposito		: Id del Usuario Caja
	 * @return				: Listado CierreCaja
	 * @throws Throwable	: Excepcions
	 */
	List<CierreCaja> selectByIdDeposito(Long idDeposito)throws Throwable;
	
}
