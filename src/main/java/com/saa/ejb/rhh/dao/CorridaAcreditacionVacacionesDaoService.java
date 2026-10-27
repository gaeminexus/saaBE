package com.saa.ejb.rhh.dao;

import java.time.LocalDate;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.CorridaAcreditacionVacaciones;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService CorridaAcreditacionVacaciones.
 */
@Local
public interface CorridaAcreditacionVacacionesDaoService extends EntityDao<CorridaAcreditacionVacaciones> {

	/**
	 * Recupera la ultima corrida registrada de una empresa, sea cual sea su estado.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @return				: La corrida mas reciente, o null si nunca se registro ninguna
	 * @throws Throwable	: Excepcion
	 */
	CorridaAcreditacionVacaciones selectUltimaByEmpresa(Long idEmpresa) throws Throwable;

	/**
	 * Recupera la ultima corrida registrada de una empresa que termino en OK. Las ERROR y las
	 * OMITIDA no acreditaron nada, asi que no cuentan.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @return				: La corrida OK mas reciente, o null si no hay ninguna
	 * @throws Throwable	: Excepcion
	 */
	CorridaAcreditacionVacaciones selectUltimaOkByEmpresa(Long idEmpresa) throws Throwable;

	/**
	 * Indica si ya hay una corrida OK del mismo anio con fecha de corte POSTERIOR a la dada.
	 * Acreditar recalcula los dias asignados al corte, asi que correr con un corte anterior
	 * bajaria los dias de todos.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio del saldo
	 * @param fechaCorte	: Corte que se pretende usar
	 * @return				: true si existe una corrida OK del anio con corte mayor
	 * @throws Throwable	: Excepcion
	 */
	boolean existeOkConCortePosterior(Long idEmpresa, Integer anio, LocalDate fechaCorte) throws Throwable;
}
