package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.LiquidacionBeneficioSocial;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService LiquidacionBeneficioSocial.
 */
@Local
public interface LiquidacionBeneficioSocialDaoService extends EntityDao<LiquidacionBeneficioSocial> {


	/**
	 * Recupera el beneficio de un empleado para un tipo y un anio.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param tipoBeneficio	: Codigo alterno del detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL
	 * @param anio			: Anio del beneficio
	 * @return				: El beneficio, o null si aun no se genero
	 * @throws Throwable	: Excepcion
	 */
	LiquidacionBeneficioSocial selectByEmpleadoTipoAnio(Long idEmpleado, Long tipoBeneficio,
			Integer anio) throws Throwable;
}
