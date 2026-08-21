package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.FormatoArchivoBancario;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad FormatoArchivoBancario.
 *  Accede a los metodos DAO y procesa los datos para el FormatoArchivoBancario.</p>
 */
@Local
public interface FormatoArchivoBancarioService extends EntityService<FormatoArchivoBancario> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	FormatoArchivoBancario selectById(Long id) throws Throwable;

}
