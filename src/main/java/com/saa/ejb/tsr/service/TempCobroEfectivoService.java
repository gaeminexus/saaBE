package com.saa.ejb.tsr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.TempCobroEfectivo;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempCobroEfectivo.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface TempCobroEfectivoService extends EntityService<TempCobroEfectivo>{

	/**
	 * Elimina cobros con efectivo temporales
	 * @param idTempCobro	: id del cobro
	 * @throws Throwable	: Excepcion
	 */
	 void eliminaCobroEfectivoByIdCobro (Long idTempCobro) throws Throwable; 

}
