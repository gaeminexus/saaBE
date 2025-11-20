package com.saa.ejb.tesoreria.service;

import java.util.List;
import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.Cheque;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Cheque.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface ChequeService extends EntityService<Cheque>{
	
	/**
	 * Recupera el siguente cheque disponible de una cuenta bancaria 
	 * @param cuenta		: Numero de Cuenta
	 * @return
	 * @throws Throwable	: Excepcion
	 */
	Long recuperaSiguienteCheque(Long cuenta) throws Throwable;
	
	/**
	 * Crea los cheques solicitados por una chequera
	 * @param idChequera	: Id de la chequera
	 * @param totalCheques	: Numero de cheques solicitados
	 * @param chequeInicial	: Numero de cheque inicial de la secuencua de cheques
	 * @throws Throwable	: Excepcion
	 */
	void crearChequesDeChequera(Long idChequera, Long totalCheques, Long chequeInicial) throws Throwable;
	
	/**
	 * Recupera el cheque cheque disponible de una cuenta bancaria
	 * @param idCuenta		: Id de Cuenta Bancaria
	 * @return				: Datos del Cheque = Id y numero de cheque 
	 * @throws Throwable	: Excepcion
	 */
	Long[] recuperaPrimerCheque(Long idCuenta) throws Throwable;
	
	/**
	 * Proceso para la impresion de cheques
	 * @param idEmpresa			: Id de Empresa
	 * @param idCheque			: Id del cheque
	 * @param idCuentaBancaria	: Id de cuenta bancaria
	 * @param idPersona			: Id del Proveedor
	 * @param nombreUsuario		: Nombre del usuario
	 * @param valor				: Valor depositado
	 * @param idBeneficiario	: Id beneficiario
	 * @param beneficiario		: Nombre del beneficiario
	 * @return					: Arreglo con id de asiento y mensaje de salida
	 * @throws Throwable		: Excepcion
	 */
	String[] procesoImpresionCheques(Long idEmpresa, Long idCheque, Long idCuentaBancaria, 
		Long idPersona, String nombreUsuario, Double valor, Long idBeneficiario, String beneficiario)throws Throwable;
	
	/**
	 * Genera el asiento contable para la impresion de cheques
	 * @param idEmpresa			: Id de Empresa
	 * @param idCheque			: Id del cheque
	 * @param idCuentaBancaria	: Id de cuenta bancaria
	 * @param idPersona			: Id del beneficiario
	 * @param nombreUsuario		: Nombre del usuario
	 * @param valor				: Valor depositado
	 * @param beneficiario		: Nombre del beneficiario que se imprimio en el cheque 
	 * @return					: Arreglo con id de asiento y mensaje d salida
	 * @throws Throwable		: Excepcion
	 */
	Long[] generaAsientoImpresion(Long idEmpresa, Long idCheque, Long idCuentaBancaria, Long idPersona, 
		String nombreUsuario, Double valor, String beneficiario)throws Throwable;
	
	/**
	 * Inserta el detalle del Haber del asiento contable
	 * @param idAsiento		: Id de asiento
	 * @param cheque		: Entidad cheques
	 * @param observacion	: Observacion
	 * @throws Throwable	: Excepcion
	 */
	void insertarDetalleAsientoDebe(Long idAsiento, Cheque cheque, String observacion)throws Throwable;
	
	/**
	 * Inserta el detalle del Haber del asiento contable
	 * @param idAsiento			: Id de asiento
	 * @param idCuentaBancaria	: Id de cuenta bancaria
	 * @param observacion		: Observacion
	 * @param valor				: Valor a ingresar
	 * @throws Throwable		: Excepcion
	 */
	void insertarDetalleAsientoHaber(Long idAsiento, Long idCuentaBancaria, String observacion, Double valor)throws Throwable;
	
	/**
	 * Servico de actualizacion del cheque una vez impreso
	 * @param idCheque			:Id Cheque
	 * @throws Throwable		:Excepcion
	 */
	void impresionFisicaCheque(Long idCheque) throws Throwable;
	
	
	/**
	 * Proceso para actualizar datos de control cuando se entrega cheque
	 * @param cheques			:Listado de Ids de cheques
	 * @throws Throwable		:Excepcion 
	 */
	void actualizaChequeEntregado(List<Long> cheques) throws Throwable;
	
	/**
	 * Proceso que reversa la generacion del cheque
	 * @param idCheque			:Id del cheque
	 * @throws Throwable		:Excepcion
	 */
	void reversarChequeGenerado(Long idCheque) throws Throwable;
	
	/**
	 * Reversa la impresión hasta revertir generacion de cheque
	 * @param idCheque			:Id del cheque
	 * @throws Throwable		:Excepcion
	 */
	void reversarChequeImpreso(Long idCheque) throws Throwable;
	
	/**
	 * Reversa la impresión hasta revertir generacion de cheque
	 * @param idCheque			:Id del Cheque
	 * @throws Throwable		:Excepcion
	 */
	void reversarChequeEntregado(Long idCheque) throws Throwable;	
	
}
