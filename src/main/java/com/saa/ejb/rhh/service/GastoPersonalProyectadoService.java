package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.GastoPersonalProyectado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad GastoPersonalProyectado.
 *  Accede a los metodos DAO y procesa los datos para el GastoPersonalProyectado.</p>
 */
@Local
public interface GastoPersonalProyectadoService extends EntityService<GastoPersonalProyectado> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	GastoPersonalProyectado selectById(Long id) throws Throwable;

}
