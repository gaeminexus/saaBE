package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.TempCobroTarjeta;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempCobroTransferencia.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface TempCobroTarjetaService extends EntityService<TempCobroTarjeta>{
	
	/**
	 * Elimina cobros con tarjeta temporales
	 * @param idTempCobro	: Id del cobro
	 * @throws Throwable	: Excepcions
	 */
	 void eliminaCobroTarjetaByIdCobro (Long idTempCobro) throws Throwable;
	
}