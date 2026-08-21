package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.GastoPersonalProyectado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService GastoPersonalProyectado.
 */
@Local
public interface GastoPersonalProyectadoDaoService extends EntityDao<GastoPersonalProyectado> {


	/**
	 * Suma los gastos personales vigentes declarados por un empleado para un anio.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @return				: La suma, o cero si no declaro nada
	 * @throws Throwable	: Excepcion
	 */
	Double sumaVigentes(Long idEmpleado, Integer anio) throws Throwable;
}
