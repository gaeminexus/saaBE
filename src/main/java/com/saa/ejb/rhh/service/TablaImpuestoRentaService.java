package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.TablaImpuestoRenta;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TablaImpuestoRenta.
 *  Accede a los metodos DAO y procesa los datos para el TablaImpuestoRenta.</p>
 */
@Local
public interface TablaImpuestoRentaService extends EntityService<TablaImpuestoRenta> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	TablaImpuestoRenta selectById(Long id) throws Throwable;

}
