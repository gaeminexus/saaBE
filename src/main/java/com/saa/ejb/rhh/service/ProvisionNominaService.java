package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.ProvisionNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad ProvisionNomina.
 *  Accede a los metodos DAO y procesa los datos para el ProvisionNomina.</p>
 */
@Local
public interface ProvisionNominaService extends EntityService<ProvisionNomina> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	ProvisionNomina selectById(Long id) throws Throwable;

}
