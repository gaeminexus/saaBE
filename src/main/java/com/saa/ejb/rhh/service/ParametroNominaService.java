package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.ParametroNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad ParametroNomina.
 *  Accede a los metodos DAO y procesa los datos para el ParametroNomina.</p>
 */
@Local
public interface ParametroNominaService extends EntityService<ParametroNomina> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	ParametroNomina selectById(Long id) throws Throwable;

}
