package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.NovedadNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad NovedadNomina.
 *  Accede a los metodos DAO y procesa los datos para el NovedadNomina.</p>
 */
@Local
public interface NovedadNominaService extends EntityService<NovedadNomina> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	NovedadNomina selectById(Long id) throws Throwable;

}
