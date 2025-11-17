package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.TempCobroTransferencia;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempCobroTransferencia.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface TempCobroTransferenciaService extends EntityService<TempCobroTransferencia>{
	 
	/**
	 * Elimina cobros con Transferencias temporales 
	 * @param idTempCobro	: Id del cobro
	 * @throws Throwable	: Excepcions
	 */
	 void eliminaCobroTransferenciaByIdCobro (Long idTempCobro) throws Throwable;

}