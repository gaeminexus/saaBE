package com.saa.ejb.cnt.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.MatchCuenta;

import jakarta.ejb.Local;

@Local
public interface MatchCuentaService extends EntityService<MatchCuenta> {

	 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  MatchCuenta selectById(Long id) throws Throwable;

}
