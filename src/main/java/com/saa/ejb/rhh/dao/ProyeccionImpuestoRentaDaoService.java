package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.ProyeccionImpuestoRenta;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ProyeccionImpuestoRenta.
 */
@Local
public interface ProyeccionImpuestoRentaDaoService extends EntityDao<ProyeccionImpuestoRenta> {


	/**
	 * Recupera la proyeccion vigente de un empleado para un anio.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @return				: La proyeccion vigente, o null si aun no se proyecto
	 * @throws Throwable	: Excepcion
	 */
	ProyeccionImpuestoRenta selectVigente(Long idEmpleado, Integer anio) throws Throwable;

	/**
	 * Marca como no vigentes todas las proyecciones de un empleado en un anio. Se
	 * invoca antes de insertar la nueva.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @return				: Numero de proyecciones desmarcadas
	 * @throws Throwable	: Excepcion
	 */
	int desmarcaVigentes(Long idEmpleado, Integer anio) throws Throwable;
}
