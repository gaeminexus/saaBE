package com.saa.ejb.tsr.service;

import java.util.List;
import com.saa.basico.util.EntityService;
import com.saa.model.cnt.Asiento;
import com.saa.model.tsr.Pago;
import com.saa.model.tsr.TempPago;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Persona.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface PagoService extends EntityService<Pago> {

	/**	
	 * Almacena un pago ingresado
	 * @param idTempPago	: Id de temporal de Pago
	 * @return			 	: Arreglo con el id de asiento, numero de asiento, id de pago y mensaje de ingreso
	 * @throws Throwable 	: Excepcion
	 */
	String[] crearPagoIngresado(Long idTempPago) throws Throwable;
	
	/**
	 * Copia el pago temporal a un pago real
	 * 
	 * @param idTempPago	: Id de temporal de Pago
	 * @return				: Pago ingresado
	 * @throws Throwable 	: Excepcion 
	 */
	Pago copiarPagoTemporalAReal(Long idTempPago) throws Throwable;
	
	/**
	 * Almacena losdatos de un pago en la tabla real
	 * 
	 * @param tempPago		: Pago Temporal
	 * @return				: Pago
	 * @throws Throwable	: Excepcion
	 */
	Pago savePagoReal(TempPago tempPago) throws Throwable;
	
	/**
	 * Crea el asiento para el pago
	 * @param pago			: Pago del que se registrara el asiento
	 * @return				: El numero y id del asiento
	 * @throws Throwable	: Excepcion
	 */
	Long[] crearAsientoPago(Pago pago) throws Throwable;
	
	/**
	 * Recupera un pago por el Id de Cheque 
	 * @param idCheque 		: Id del Cheque
	 * @return				: El pago
	 * @throws Throwable	: Excepcion
	 */	
	List<Pago> recuperarPagoIdCheque(Long idCheque)throws Throwable;
	
	/**
	 * Proceso de reversion de un ingreso de un pago
	 * @param idPago		: Id del Pago
	 * @param motivoAnular	: Codigo del motivo de anulacion
	 * @throws Throwable	: Excepcion
	 */
	void anularIngresoPago(Long idPago, int motivoAnular) throws Throwable;
	
	/**
	 * Valida si se puede anular el pago
	 * @param pago			: Pago
	 * @param asiento		: Asiento
	 * @return				: Verdadero-Si sepuede o False-No se puede
	 * @throws Throwable	:Excepcion
	 */
	public boolean validaAnularPago(Pago pago, Asiento asiento) throws Throwable;
	
	/**
	 * Actualiza idCheque y estado por idCheque
	 * @param idCheque
	 * @param estado
	 * @throws Throwable
	 */
	void updateEstadoIdChequeByIdCheque(Long idCheque, int estado) throws Throwable;
	
}
