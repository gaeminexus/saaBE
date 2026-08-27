package com.saa.ejb.tsr.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Cheque;
import com.saa.model.tsr.Titular;

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

	// =====================================================================
	// Integración con el circuito moderno de pagos (PGS.PGTR)
	// =====================================================================

	/**
	 * Cheque ACTIVO de menor número entre las chequeras ACTIVAS de la cuenta.
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @return					: Cheque disponible
	 * @throws Throwable		: IncomeException si la cuenta no tiene cheques disponibles
	 */
	Cheque siguienteDisponible(Long idCuentaBancaria) throws Throwable;

	/**
	 * Toma el siguiente cheque disponible de la cuenta y lo asigna a un pago:
	 * lo deja GENERADO con el valor, el titular, el beneficiario y la fecha de
	 * uso. Pasa la chequera a TERMINADA si era el último cheque disponible.
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @param valor				: Valor del cheque
	 * @param titular			: Titular al que se le paga (puede ser null)
	 * @param beneficiario		: Nombre del beneficiario que se imprime en el cheque
	 * @param idUsuario			: Id del usuario que registra
	 * @return					: Cheque asignado, en estado GENERADO
	 * @throws Throwable		: Excepcion
	 */
	Cheque asignarAPago(Long idCuentaBancaria, Double valor, Titular titular, String beneficiario,
			Long idUsuario) throws Throwable;

	/**
	 * Anula un cheque suelto que todavía está ACTIVO (no asignado a ningún
	 * pago). Si el cheque tiene un PagoProgramado asociado se rechaza: hay que
	 * reversar el pago, no anular el cheque directamente.
	 * @param idCheque	: Id del cheque
	 * @param motivo	: Código del motivo (rubro MotivoAnulacionCheque)
	 * @param idUsuario	: Id del usuario que anula
	 * @throws Throwable: Excepcion
	 */
	void anularChequeSuelto(Long idCheque, Long motivo, Long idUsuario) throws Throwable;

	/**
	 * Anula un cheque por la reversión de su pago (motivo PAGO_REVERSADO). El
	 * cheque anulado no se reutiliza.
	 * @param idCheque	: Id del cheque
	 * @throws Throwable: Excepcion
	 */
	void anularPorReverso(Long idCheque) throws Throwable;

	/**
	 * Marca como IMPRESOS los cheques indicados (deben estar GENERADO). Si
	 * alguno no está en ese estado, aborta toda la operación.
	 * @param ids		: Ids de los cheques
	 * @param idUsuario	: Id del usuario que imprime
	 * @throws Throwable: Excepcion
	 */
	void marcarImpresos(List<Long> ids, Long idUsuario) throws Throwable;

	/**
	 * Marca como ENTREGADOS los cheques indicados (deben estar IMPRESO). Si
	 * alguno no está en ese estado, aborta toda la operación.
	 * @param ids		: Ids de los cheques
	 * @param idUsuario	: Id del usuario que entrega
	 * @throws Throwable: Excepcion
	 */
	void marcarEntregados(List<Long> ids, Long idUsuario) throws Throwable;

	/**
	 * Listado de cheques con los datos del pago que los usó, para la pantalla
	 * de consulta de cheques.
	 * @param idEmpresa			: Id de la empresa (sólo filtra los cheques con pago asociado)
	 * @param idCuentaBancaria	: Id de la cuenta bancaria, null para todas
	 * @param estado			: Estado del cheque (rubro EstadoCheque), null para todos
	 * @param desde				: Fecha de uso desde, null sin límite inferior
	 * @param hasta				: Fecha de uso hasta, null sin límite superior
	 * @return					: Listado con idCheque, numero, estado, valor, beneficiario,
	 * 							  fechaUso, fechaImpresion, fechaEntrega, idPago, tipoPago,
	 * 							  referenciaPago, numeroCuenta, banco
	 * @throws Throwable		: Excepcion
	 */
	List<Map<String, Object>> listar(Long idEmpresa, Long idCuentaBancaria, Long estado,
			LocalDate desde, LocalDate hasta) throws Throwable;

}
