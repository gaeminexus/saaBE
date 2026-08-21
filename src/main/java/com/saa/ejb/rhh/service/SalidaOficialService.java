package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.SalidaOficial;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad SalidaOficial.
 *  Accede a los metodos DAO y procesa los datos para el SalidaOficial.</p>
 */
@Local
public interface SalidaOficialService extends EntityService<SalidaOficial> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	SalidaOficial selectById(Long id) throws Throwable;

}
