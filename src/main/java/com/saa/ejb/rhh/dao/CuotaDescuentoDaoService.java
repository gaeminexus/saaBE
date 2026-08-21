package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.CuotaDescuento;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService CuotaDescuento.
 */
@Local
public interface CuotaDescuentoDaoService extends EntityDao<CuotaDescuento> {


	/**
	 * Recupera las cuotas pendientes de un empleado que vencen dentro de un rango,
	 * de descuentos que siguen vigentes.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param desde			: Fecha de inicio del rango
	 * @param hasta			: Fecha de fin del rango
	 * @return				: Listado de cuotas; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<CuotaDescuento> selectPendientesPorVencer(Long idEmpleado, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable;
}
