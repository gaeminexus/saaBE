package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.MatchCuenta;

import jakarta.ejb.Remote;

@Remote
public interface MatchCuentaService extends EntityService<MatchCuenta> {

	 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  MatchCuenta selectById(Long id) throws Throwable;

}
