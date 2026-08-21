package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.OrdenPagoNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad OrdenPagoNomina.
 *  Accede a los metodos DAO y procesa los datos para el OrdenPagoNomina.</p>
 */
@Local
public interface OrdenPagoNominaService extends EntityService<OrdenPagoNomina> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	OrdenPagoNomina selectById(Long id) throws Throwable;

}
