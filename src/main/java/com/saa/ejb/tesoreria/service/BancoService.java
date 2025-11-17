package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.Banco;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Banco.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface BancoService extends EntityService<Banco>{

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  Banco selectById(Long id) throws Throwable;
	
}