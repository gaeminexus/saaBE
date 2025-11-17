package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.Asiento;
import com.saa.model.tesoreria.CajaFisica;
import com.saa.model.tesoreria.Deposito;
import com.saa.model.tesoreria.UsuarioPorCaja;

import jakarta.ejb.Remote;
	
/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Deposito.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface DepositoService extends EntityService<Deposito>{
 	 
	/**
	 * Genera el deposito real
	 * @param idUsuarioCaja: Id usuario por caja
	 * @return				: Id deposito
	 * @throws Throwable	: Excepcion
	 */
	Long generaDepositoReal(Long idUsuarioCaja) throws Throwable;
	 
	/**
	 * Genera regsitro de deposito
	 * @param usuarioPorCaja: Id usuario por caja
	 * @param efectivo		: valor de efectivo
	 * @param cheque		: valor de cheque
	 * @return				: Id de deposito
	 * @throws Throwable	: Excepcion
	 */
	Deposito generaRegistroDeposito(UsuarioPorCaja usuarioPorCaja, Double efectivo, Double cheque) throws Throwable;
	 
	/**
	 * actualizacion de estados de dpst y de crcj y cchq
	 * @param idEmpresa		: Id de empresa
	 * @param idDeposito	: Id de deposito
	 * @param idCajaFisica	: Id de cierre de caja
	 * @param nombreUsuario	: Nombre del usuario
	 * @throws Throwable	: Excepcion
	 */
	void actualizaDatosDeposito(Long idEmpresa, Long idDeposito, Long idCajaFisica, String nombreUsuario) throws Throwable;
	
	/**
	 * Genera asiento para descargar caja contable transitoria 
	 * @param idEmpresa	: Id de la Empresa
	 * @param deposito		: Entidad Deposito
	 * @param cajaFisica	: Entidad Caja Fisisca
	 * @param valor		: Valor depositado
	 * @return				: Entidad asiento
	 * @throws Throwable	: Excepcion
	 */
	Asiento generaAsientoCajaContableTransitoria(Long idEmpresa, Deposito deposito, CajaFisica cajaFisica, Double valor) throws Throwable;
	
	/**
	 * Reversion de un envio de deposito al banco
	 * @param idDeposito: Id de deposito
	 * @throws Throwable: Excepcion
	 */
	void reversarDepositoBanco(Long idDeposito) throws Throwable;
	
	/**
	 * Inserta los cheques y el efectivo de todos los cierres en auxiliar de deposito
	 * @param idUsuarioCaja: Id usuario caja
	 * @return				: Valor de efectivo
	 * @throws Throwable	: Excepcion
	 */
	Double prepararTemporalDepositoDesglose(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Inserta en tabla temporal los cierres a enviar a depositar
	 * @param idUsuarioCaja	: Id de usuario por caja
	 * @throws Throwable	: Excepcion
	 */
	void prepararTemporalDepositoCierre(Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Reversa el un deposito ratificado
	 * @param idDeposito		: Id del deposito
	 * @param idDetalleDeposito	: Id del detalle del deposito
	 * @return					: Retorna el id del asiento
	 * @throws Throwable		: Excepcion
	 */
	Long reversaRatificacionDeposito(Long idDeposito, Long idDetalleDeposito)throws Throwable;
	
	/**
	 * Anula el o los asientos contables generados en la ratifciacion
	 * @param idDetalleDeposito	: Id del detalle del deposito
	 * @return					: Retorna el id del asiento
	 * @throws Throwable		: Excepcion
	 */
	Asiento anulaAsientoRatificacion(Long idDetalleDeposito)throws Throwable;
	
	/**
	 * actualiza campos de control de la ratificacion
	 * @param idDeposito		: Id del deposito
	 * @param idDetalleDeposito	: Id del detalle del deposito
	 * @throws Throwable		: Excepcion
	 */
	void actualizaDatosRatificacion(Long idDeposito, Long idDetalleDeposito)throws Throwable;
	
	/**
	 * Actualiza el estado del deposito
	 * @param deposito	: Deposito
	 * @param asiento	: Entidad asiento
	 * @param estado	: Estado al que se cambiara
	 * @throws Throwable: Excepcion
	 */
	void actualizaEstadoDetosito(Deposito deposito, Asiento asiento, int estado) throws Throwable;
	
}