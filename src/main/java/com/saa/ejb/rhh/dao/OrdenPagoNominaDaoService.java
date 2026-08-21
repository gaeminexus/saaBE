package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.OrdenPagoNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService OrdenPagoNomina.
 */
@Local
public interface OrdenPagoNominaDaoService extends EntityDao<OrdenPagoNomina> {

	/**
	 * Recupera las ordenes de pago de un periodo, de la mas reciente a la mas antigua.
	 *
	 * <p>Un periodo puede tener mas de una orden: si el banco rechaza acreditaciones, se
	 * emite otra por los rechazados. Por eso devuelve lista y no una sola.</p>
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: Ordenes del periodo
	 * @throws Throwable	: Excepcion
	 */
	List<OrdenPagoNomina> selectByPeriodo(Long idPeriodo) throws Throwable;

	/**
	 * Recupera las ordenes de un periodo que todavia no tienen fecha de acreditacion.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: Ordenes emitidas y sin confirmar
	 * @throws Throwable	: Excepcion
	 */
	List<OrdenPagoNomina> selectPendientesByPeriodo(Long idPeriodo) throws Throwable;

}
