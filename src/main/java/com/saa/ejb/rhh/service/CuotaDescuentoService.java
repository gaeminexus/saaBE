package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.CuotaDescuento;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CuotaDescuento.
 *  Accede a los metodos DAO y procesa los datos para el CuotaDescuento.</p>
 */
@Local
public interface CuotaDescuentoService extends EntityService<CuotaDescuento> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	CuotaDescuento selectById(Long id) throws Throwable;

}
