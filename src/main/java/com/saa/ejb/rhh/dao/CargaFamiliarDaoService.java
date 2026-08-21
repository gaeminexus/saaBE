package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.CargaFamiliar;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService CargaFamiliar.
 */
@Local
public interface CargaFamiliarDaoService extends EntityDao<CargaFamiliar> {


	/**
	 * Cuenta las cargas familiares de un empleado que califican para la rebaja de
	 * gastos personales y estan vigentes a una fecha.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param fecha			: Fecha de corte de la vigencia
	 * @return				: Numero de cargas; cero si no hay
	 * @throws Throwable	: Excepcion
	 */
	Integer contarVigentesParaIr(Long idEmpleado, java.time.LocalDate fecha) throws Throwable;
}
