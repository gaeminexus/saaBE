package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.DetalleLiquidacionExterna;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DetalleLiquidacionExterna.
 *  Accede a los metodos DAO y procesa los datos para el DetalleLiquidacionExterna.</p>
 *
 * <p>El alta/edicion en bloque de los detalles la maneja
 * {@link LiquidacionExternaService#registrar} / {@link LiquidacionExternaService#actualizar};
 * este servicio queda para el CRUD estandar de la entidad.</p>
 */
@Local
public interface DetalleLiquidacionExternaService extends EntityService<DetalleLiquidacionExterna> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	DetalleLiquidacionExterna selectById(Long id) throws Throwable;

}
