package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.ProyeccionImpuestoRenta;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad ProyeccionImpuestoRenta.
 *  Accede a los metodos DAO y procesa los datos para el ProyeccionImpuestoRenta.</p>
 */
@Local
public interface ProyeccionImpuestoRentaService extends EntityService<ProyeccionImpuestoRenta> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	ProyeccionImpuestoRenta selectById(Long id) throws Throwable;

}
