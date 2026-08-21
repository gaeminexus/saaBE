package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.AcumuladoNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad AcumuladoNomina.
 *  Accede a los metodos DAO y procesa los datos para el AcumuladoNomina.</p>
 */
@Local
public interface AcumuladoNominaService extends EntityService<AcumuladoNomina> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	AcumuladoNomina selectById(Long id) throws Throwable;

}
