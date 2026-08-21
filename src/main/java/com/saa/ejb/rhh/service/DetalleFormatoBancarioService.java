package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.DetalleFormatoBancario;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DetalleFormatoBancario.
 *  Accede a los metodos DAO y procesa los datos para el DetalleFormatoBancario.</p>
 */
@Local
public interface DetalleFormatoBancarioService extends EntityService<DetalleFormatoBancario> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	DetalleFormatoBancario selectById(Long id) throws Throwable;

}
