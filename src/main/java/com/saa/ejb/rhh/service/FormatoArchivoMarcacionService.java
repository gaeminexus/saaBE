package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.FormatoArchivoMarcacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad FormatoArchivoMarcacion.
 *  Accede a los metodos DAO y procesa los datos para el FormatoArchivoMarcacion.</p>
 */
@Local
public interface FormatoArchivoMarcacionService extends EntityService<FormatoArchivoMarcacion> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	FormatoArchivoMarcacion selectById(Long id) throws Throwable;

}
