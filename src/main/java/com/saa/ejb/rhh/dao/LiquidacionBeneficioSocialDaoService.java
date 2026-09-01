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

	/**
	 * Liquidaciones sueltas (LQBSODBS is null) de una empresa, tipo, anio y region, en estado
	 * PENDIENTE. Es lo que agrupa {@code POST /odbs/generar}: solo estas, nunca las que ya
	 * tienen orden, para que el proceso sea repetible sin duplicar.
	 *
	 * @param idEmpresa			: Id de la empresa
	 * @param tipoBeneficio		: Codigo alterno del detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL
	 * @param anio				: Anio del beneficio
	 * @param region			: Region del decimo cuarto; null para los demas tipos
	 * @return					: Liquidaciones sueltas; vacio si no hay
	 * @throws Throwable		: Excepcion
	 */
	List<LiquidacionBeneficioSocial> selectPendientesByCombinacion(Long idEmpresa,
			Long tipoBeneficio, Integer anio, Long region) throws Throwable;

	/**
	 * Liquidaciones agrupadas bajo una orden de beneficio social, para el detalle
	 * ({@code GET /odbs/detalle/{id}}), la confirmacion del pago y la anulacion.
	 *
	 * @param idOrden			: Id de la orden (RHH.ODBS.ODBSCDGO)
	 * @return					: Liquidaciones de la orden; vacio si no hay
	 * @throws Throwable		: Excepcion
	 */
	List<LiquidacionBeneficioSocial> selectByOrden(Long idOrden) throws Throwable;
}
