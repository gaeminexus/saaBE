package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.Utilidad;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService Utilidad.
 */
@Local
public interface UtilidadDaoService extends EntityDao<Utilidad> {

	/**
	 * Recupera el reparto de un ejercicio, si ya existe.
	 *
	 * <p>Es lo que hace idempotente a <code>calcular</code>: el unique
	 * <code>UQ_UTLD_ANIO (PJRQCDGO, UTLDANOO)</code> impide dos repartos del mismo anio, asi
	 * que recalcular actualiza en vez de duplicar.</p>
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Ejercicio fiscal
	 * @return				: El reparto, o null
	 * @throws Throwable	: Excepcion
	 */
	Utilidad selectByEmpresaYAnio(Long idEmpresa, Integer anio) throws Throwable;

}
