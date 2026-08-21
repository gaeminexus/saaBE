package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.TopeGastoPersonal;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TopeGastoPersonal.
 *  Accede a los metodos DAO y procesa los datos para el TopeGastoPersonal.</p>
 */
@Local
public interface TopeGastoPersonalService extends EntityService<TopeGastoPersonal> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	TopeGastoPersonal selectById(Long id) throws Throwable;

}
