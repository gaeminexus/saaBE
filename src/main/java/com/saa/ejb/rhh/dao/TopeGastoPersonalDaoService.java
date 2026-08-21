package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.TopeGastoPersonal;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService TopeGastoPersonal.
 */
@Local
public interface TopeGastoPersonalDaoService extends EntityDao<TopeGastoPersonal> {


	/**
	 * Recupera el tope de canastas para un numero de cargas. Si el numero supera el
	 * maximo parametrizado, devuelve la fila del maximo, que es la regla del SRI.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio fiscal
	 * @param numeroCargas	: Numero de cargas familiares
	 * @return				: El tope aplicable, o null si el anio no esta parametrizado
	 * @throws Throwable	: Excepcion
	 */
	TopeGastoPersonal selectByCargas(Long idEmpresa, Integer anio, Integer numeroCargas) throws Throwable;
}
