package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.DetalleOrdenPagoNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DetalleOrdenPagoNomina.
 *  Accede a los metodos DAO y procesa los datos para el DetalleOrdenPagoNomina.</p>
 */
@Local
public interface DetalleOrdenPagoNominaService extends EntityService<DetalleOrdenPagoNomina> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	DetalleOrdenPagoNomina selectById(Long id) throws Throwable;

}
