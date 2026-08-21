package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.CuentaBancariaEmpleado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService CuentaBancariaEmpleado.
 */
@Local
public interface CuentaBancariaEmpleadoDaoService extends EntityDao<CuentaBancariaEmpleado> {

	/**
	 * Recupera las cuentas activas de un empleado, con la principal primero.
	 *
	 * <p>El orden importa: la orden de pago acredita a la principal salvo que el empleado
	 * reparta su sueldo entre varias por <code>CBEMPRCN</code>, y en ese caso la principal
	 * es la que se lleva el residuo del redondeo.</p>
	 *
	 * @param idEmpleado	: Id del empleado
	 * @return				: Cuentas activas del empleado
	 * @throws Throwable	: Excepcion
	 */
	List<CuentaBancariaEmpleado> selectActivasByEmpleado(Long idEmpleado) throws Throwable;

}
