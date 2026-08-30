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

	/**
	 * Recupera la novedad de un empleado y concepto cuya descripcion coincide
	 * exactamente. Es la via con la que {@code SolicitudVacacionesServiceImpl} ubica,
	 * al anular una aprobacion, la novedad que ella misma creo -- no hay FK de SLCT a
	 * NVNM, la novedad se marca con "Solicitud de vacaciones #{codigo}" como descripcion.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param idConcepto	: Id del concepto de nomina
	 * @param descripcion	: Descripcion exacta a buscar
	 * @return				: La novedad, o null si no existe
	 * @throws Throwable	: Excepcion
	 */
	NovedadNomina selectPorDescripcion(Long idEmpleado, Long idConcepto, String descripcion) throws Throwable;
}
