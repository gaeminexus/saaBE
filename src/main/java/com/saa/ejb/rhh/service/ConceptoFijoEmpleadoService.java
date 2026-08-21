package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.ConceptoFijoEmpleado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad ConceptoFijoEmpleado.
 *  Accede a los metodos DAO y procesa los datos para el ConceptoFijoEmpleado.</p>
 */
@Local
public interface ConceptoFijoEmpleadoService extends EntityService<ConceptoFijoEmpleado> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	ConceptoFijoEmpleado selectById(Long id) throws Throwable;

}
