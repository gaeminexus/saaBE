package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.TempCobroTransferencia;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempCobroTransferencia.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface TempCobroTransferenciaService extends EntityService<TempCobroTransferencia>{
	 
	/**
	 * Elimina cobros con Transferencias temporales 
	 * @param idTempCobro	: Id del cobro
	 * @throws Throwable	: Excepcions
	 */
	 void eliminaCobroTransferenciaByIdCobro (Long idTempCobro) throws Throwable;

}
