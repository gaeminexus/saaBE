package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.Utilidad;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Utilidad.
 *  Accede a los metodos DAO y procesa los datos para el Utilidad.</p>
 */
@Local
public interface UtilidadService extends EntityService<Utilidad> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	Utilidad selectById(Long id) throws Throwable;

}
