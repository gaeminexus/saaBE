package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.TempCobroCheque;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempCobroCheque.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface TempCobroChequeService extends EntityService<TempCobroCheque>{
 	 
	/**
	 * Elimina cobros con cheques temporales 
	 * @param idTempCobro	: Id de cobro
	 * @throws Throwable	: Excepcions
	 */
	 void eliminaCobroChequeByIdCobro (Long idTempCobro) throws Throwable;

	 
}
