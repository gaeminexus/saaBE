package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.DetalleFormatoMarcacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DetalleFormatoMarcacion.
 *  Accede a los metodos DAO y procesa los datos para el DetalleFormatoMarcacion.</p>
 */
@Local
public interface DetalleFormatoMarcacionService extends EntityService<DetalleFormatoMarcacion> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	DetalleFormatoMarcacion selectById(Long id) throws Throwable;

}
