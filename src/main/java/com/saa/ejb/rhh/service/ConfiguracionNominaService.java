package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.ConfiguracionNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad ConfiguracionNomina.
 *  Accede a los metodos DAO y procesa los datos para el ConfiguracionNomina.</p>
 */
@Local
public interface ConfiguracionNominaService extends EntityService<ConfiguracionNomina> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	ConfiguracionNomina selectById(Long id) throws Throwable;

}
