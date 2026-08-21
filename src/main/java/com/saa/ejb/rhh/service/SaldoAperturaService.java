package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.SaldoApertura;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad SaldoApertura.
 *  Accede a los metodos DAO y procesa los datos para el SaldoApertura.</p>
 */
@Local
public interface SaldoAperturaService extends EntityService<SaldoApertura> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	SaldoApertura selectById(Long id) throws Throwable;

}
