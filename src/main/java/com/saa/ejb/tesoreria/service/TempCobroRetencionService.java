package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.TempCobroRetencion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempCobroRetencion.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface TempCobroRetencionService extends EntityService<TempCobroRetencion>{
	
	/**
	 * Elimina cobros con retenciones temporales
	 * @param idTempCobro	: Id del cobro
	 * @throws Throwable	: Excepcions
	 */
	 void eliminaCobroRetencionByIdCobro (Long idTempCobro) throws Throwable;
	
}
