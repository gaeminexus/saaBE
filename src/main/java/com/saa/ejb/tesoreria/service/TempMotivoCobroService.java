package com.saa.ejb.tesoreria.service;
import com.saa.basico.util.EntityService;
import com.saa.model.tsr.TempMotivoCobro;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempMotivoCobro.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface TempMotivoCobroService extends EntityService<TempMotivoCobro>{
 	 
	/**
	 * Elimina Motivos de Cobro temporales
	 * @param idTempCobro	: Id del cobro
	 * @throws Throwable	: Excepcions
	 */
	 void eliminaMotivosCobroByIdCobro (Long idTempCobro) throws Throwable;
	
}
