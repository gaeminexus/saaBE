package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.PlanillaIess;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService PlanillaIess.
 */
@Local
public interface PlanillaIessDaoService extends EntityDao<PlanillaIess> {

	/**
	 * Planillas de un periodo, ordenadas por tipo (hasta cuatro: rol normal,
	 * quirografarios, hipotecarios, fondos de reserva).
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: Las planillas del periodo, o lista vacia
	 * @throws Throwable	: Excepcion
	 */
	List<PlanillaIess> selectByPeriodo(Long idPeriodo) throws Throwable;

	/**
	 * Indica si ya existe una planilla no anulada de ese periodo y tipo. El
	 * comprobante del portal es unico por empresa y tipo, asi que solo puede
	 * haber una planilla activa por periodo y tipo.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @param tipo			: Tipo de planilla (rubro 330)
	 * @return				: true si ya existe una planilla activa (estado distinto de ANULADA)
	 * @throws Throwable	: Excepcion
	 */
	boolean existeActivaEnPeriodoTipo(Long idPeriodo, Long tipo) throws Throwable;

}
