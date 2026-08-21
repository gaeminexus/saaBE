package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.NovedadNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService NovedadNomina.
 */
@Local
public interface NovedadNominaDaoService extends EntityDao<NovedadNomina> {


	/**
	 * Recupera las novedades aprobadas de un empleado en un periodo. El motor solo
	 * considera las que tienen NVNMAPRB='S'.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @param idEmpleado	: Id del empleado
	 * @return				: Listado de novedades; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<NovedadNomina> selectAprobadas(Long idPeriodo, Long idEmpleado) throws Throwable;
}
