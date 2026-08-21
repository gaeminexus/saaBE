package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.DescuentoRecurrente;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DescuentoRecurrente.
 *  Accede a los metodos DAO y procesa los datos para el DescuentoRecurrente.</p>
 */
@Local
public interface DescuentoRecurrenteService extends EntityService<DescuentoRecurrente> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	DescuentoRecurrente selectById(Long id) throws Throwable;

}
