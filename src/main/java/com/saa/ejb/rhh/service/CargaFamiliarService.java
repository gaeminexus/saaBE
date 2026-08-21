package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.CargaFamiliar;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CargaFamiliar.
 *  Accede a los metodos DAO y procesa los datos para el CargaFamiliar.</p>
 */
@Local
public interface CargaFamiliarService extends EntityService<CargaFamiliar> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	CargaFamiliar selectById(Long id) throws Throwable;

}
