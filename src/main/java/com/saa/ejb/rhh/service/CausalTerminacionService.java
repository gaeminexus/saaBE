package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.CausalTerminacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CausalTerminacion.
 *  Accede a los metodos DAO y procesa los datos para el CausalTerminacion.</p>
 */
@Local
public interface CausalTerminacionService extends EntityService<CausalTerminacion> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	CausalTerminacion selectById(Long id) throws Throwable;

}
