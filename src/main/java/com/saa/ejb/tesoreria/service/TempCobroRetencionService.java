package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.TempCobroRetencion;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempCobroRetencion.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface TempCobroRetencionService extends EntityService<TempCobroRetencion>{
	
	/**
	 * Elimina cobros con retenciones temporales
	 * @param idTempCobro	: Id del cobro
	 * @throws Throwable	: Excepcions
	 */
	 void eliminaCobroRetencionByIdCobro (Long idTempCobro) throws Throwable;
	
}