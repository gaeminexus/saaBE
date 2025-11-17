package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.TempMotivoPago;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempMotivoPago.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface TempMotivoPagoService extends EntityService<TempMotivoPago>{
	 
	/**
	 * Elimina los Motivos de Pago temporales 
	 * @param idTempPago	: Id del pago temporal
	 * @throws Throwable	: Excepcion
	 */
	void eliminaMotivoPagoByIdPago (Long idTempPago) throws Throwable;
}
