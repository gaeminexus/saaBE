package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.HoraExtra;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad HoraExtra.
 *  Accede a los metodos DAO y procesa los datos para el HoraExtra.</p>
 */
@Local
public interface HoraExtraService extends EntityService<HoraExtra> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	HoraExtra selectById(Long id) throws Throwable;

}
