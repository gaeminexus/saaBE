package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.Asiento;
import com.saa.model.tsr.AuxDepositoBanco;
import com.saa.model.tsr.CajaFisica;
import com.saa.model.tsr.Deposito;
import com.saa.model.tsr.DetalleDeposito;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DetalleDeposito.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface DetalleDepositoService extends EntityService<DetalleDeposito>{
 	 
	/**
	 * Almacena registros del detalle de deposito
	 * @param auxDepositoBanco	: Entidad AuxDepositoBanco
	 * @param deposito			: Entidad Deposito
	 * @return					: Entidad DetalleDeposito	
	 * @throws Throwable		: Excepcion
	 */
	DetalleDeposito saveDetalleDeposito(AuxDepositoBanco auxDepositoBanco, Deposito deposito) throws Throwable;
	
	/**
	 * Ratifica el detalle de deposito
	 * @param idEmpresa	 : Id de la empresa
	 * @param idDetDeposito : Id del detalle del deposito
	 * @param idCuenta		 : Id de cuenta bancaria
	 * @param numeroDeposito: Numero del deposito
	 * @param idUsuario	 : Id del usuario que ratifica
	 * @param nombreUsuario : Nombre de Usuario que ratifica
	 * @param idCajaFisica	 : Id de la caja fisica
	 * @param valor		 : Valor depositado
	 * @throws Throwable	 : Excepcion
	 */
	Long ratificacionDetalleDeposito(Long idEmpresa, Long idDetDeposito, Long idCuenta, String numeroDeposito, Long idUsuario, String nombreUsuario, Long idCajaFisica, Double valor) throws Throwable;
	 
	/**
	 * Actualiza el estado de detalle deposito y su fecha
	 * @param detalleDeposito	: Detalle del deposito
	 * @param numeroDeposito	: Numero del deposito
	 * @param idUsuario			: Id del usuario que ratifica
	 * @param nombreUsuario		: Nombre de Usuario que ratifica
	 * @param asiento			: Entidad asiento
	 * @param estado			: Estado al que se cambiara
	 * @throws Throwable		: Excepcion
	 */
	void actualizaEstadoDetalleDeposito(DetalleDeposito detalleDeposito, String numeroDeposito, Long idUsuario, String nombreUsuario, Asiento asiento, int estado) throws Throwable;
	 
	/**
	 * Genera asiento contable de ratificación 
	 * @param idEmpresa			: Id de la Empresa
	 * @param detalleDeposito	: Entidad Detalle Deposito
	 * @param cajaFisica		: Entidad Caja Fisisca
	 * @param valor				: Valor depositado
	 * @return					: Arreglo con id de asiento y numero de asiento
	 * @throws Throwable		: Excepcion
	 */
	 Long[] generaAsientoRatificacion(Long idEmpresa, DetalleDeposito detalleDeposito, CajaFisica cajaFisica, Double valor) throws Throwable;
		
	/**
	 * Devuelve el numero de depositos no ratificados
	 * @param idDeposito: Id del deposito
	 * @return			: Numero de depositos no ratificados
	 * @throws Throwable: Excepcion
	 */
	int numeroDepositosNoRatificados(Long idDeposito) throws Throwable;
	
	/**
	 * Valida si se puede reversar la ratificacion del deposito
	 * @param idDetalleDeposito	: Id de detalle de deposito
	 * @throws Throwable		: Excepcion
	 */
	void validaReversaRatificacionDeposito(Long idDetalleDeposito)throws Throwable;
	
	/**
	 * Contabiliza el Detalle Deposito por Deposito y Estado
	 * @param idDeposito: Id del Deposito
	 * @param estado	: Estdo deposito	
	 * @return			: Conteo Depositos
	 * @throws Throwable:
	 */
	int selectByDepositoEstado (Long idDeposito, Long estado)throws Throwable;
	
}
