package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.ParametroNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ParametroNomina.
 */
@Local
public interface ParametroNominaDaoService extends EntityDao<ParametroNomina> {


	/**
	 * Recupera los parametros normativos vigentes de un anio.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio de vigencia
	 * @return				: Los parametros, o null si el anio no esta parametrizado
	 * @throws Throwable	: Excepcion
	 */
	ParametroNomina selectByAnio(Long idEmpresa, Integer anio) throws Throwable;
}
