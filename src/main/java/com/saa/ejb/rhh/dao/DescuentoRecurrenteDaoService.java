package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.DescuentoRecurrente;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DescuentoRecurrente.
 */
@Local
public interface DescuentoRecurrenteDaoService extends EntityDao<DescuentoRecurrente> {

	/**
	 * Recupera los descuentos recurrentes vigentes de un empleado, con saldo pendiente.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @return				: Descuentos vigentes
	 * @throws Throwable	: Excepcion
	 */
	List<DescuentoRecurrente> selectVigentesByEmpleado(Long idEmpleado) throws Throwable;

}
