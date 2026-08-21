package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.ConfiguracionNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ConfiguracionNomina.
 */
@Local
public interface ConfiguracionNominaDaoService extends EntityDao<ConfiguracionNomina> {


	/**
	 * Recupera la configuracion de nomina de una empresa. Es unica por empresa.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @return				: La configuracion, o null si no existe
	 * @throws Throwable	: Excepcion
	 */
	ConfiguracionNomina selectByEmpresa(Long idEmpresa) throws Throwable;
}
