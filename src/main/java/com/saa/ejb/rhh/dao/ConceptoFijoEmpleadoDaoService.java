package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.ConceptoFijoEmpleado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ConceptoFijoEmpleado.
 */
@Local
public interface ConceptoFijoEmpleadoDaoService extends EntityDao<ConceptoFijoEmpleado> {


	/**
	 * Recupera los conceptos fijos de un empleado vigentes dentro de un rango.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param desde			: Fecha de inicio del rango
	 * @param hasta			: Fecha de fin del rango
	 * @return				: Listado de conceptos fijos; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<ConceptoFijoEmpleado> selectVigentes(Long idEmpleado, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable;
}
