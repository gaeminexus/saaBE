package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.ConceptoNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad ConceptoNomina.
 *  Accede a los metodos DAO y procesa los datos para el ConceptoNomina.</p>
 */
@Local
public interface ConceptoNominaService extends EntityService<ConceptoNomina> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	ConceptoNomina selectById(Long id) throws Throwable;

}
