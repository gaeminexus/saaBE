package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.HoraExtra;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService HoraExtra.
 */
@Local
public interface HoraExtraDaoService extends EntityDao<HoraExtra> {


	/**
	 * Recupera las horas extra aprobadas de un empleado dentro de un rango de fechas y
	 * aun no pagadas en ningun periodo.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param desde			: Fecha de inicio del rango
	 * @param hasta			: Fecha de fin del rango
	 * @return				: Listado de horas extra; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<HoraExtra> selectAprobadasPendientes(Long idEmpleado, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable;
}
