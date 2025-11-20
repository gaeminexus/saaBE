package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.MotivoPago;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad MotivoPago.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface MotivoPagoService extends EntityService<MotivoPago>{
	 
}
