package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.CargaMarcaciones;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CargaMarcaciones.
 *  Accede a los metodos DAO y procesa los datos para el CargaMarcaciones.</p>
 */
@Local
public interface CargaMarcacionesService extends EntityService<CargaMarcaciones> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	CargaMarcaciones selectById(Long id) throws Throwable;

}
