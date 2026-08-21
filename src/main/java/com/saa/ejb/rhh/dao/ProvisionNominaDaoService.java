package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.ProvisionNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ProvisionNomina.
 */
@Local
public interface ProvisionNominaDaoService extends EntityDao<ProvisionNomina> {


	/**
	 * Elimina las provisiones de un periodo. El calculo es idempotente: borra y regenera.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: Numero de provisiones eliminadas
	 * @throws Throwable	: Excepcion
	 */
	int eliminaByPeriodo(Long idPeriodo) throws Throwable;

	/**
	 * Elimina las provisiones de un empleado en un periodo, para el recalculo individual.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @param idEmpleado	: Id del empleado
	 * @return				: Numero de provisiones eliminadas
	 * @throws Throwable	: Excepcion
	 */
	int eliminaByPeriodoYEmpleado(Long idPeriodo, Long idEmpleado) throws Throwable;

	/**
	 * Recupera las provisiones de un periodo, para armar el asiento.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: Listado de provisiones; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<ProvisionNomina> selectByPeriodo(Long idPeriodo) throws Throwable;
}
