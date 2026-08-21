package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.CuentaBancariaEmpleado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CuentaBancariaEmpleado.
 *  Accede a los metodos DAO y procesa los datos para el CuentaBancariaEmpleado.</p>
 */
@Local
public interface CuentaBancariaEmpleadoService extends EntityService<CuentaBancariaEmpleado> {

	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	CuentaBancariaEmpleado selectById(Long id) throws Throwable;

}
