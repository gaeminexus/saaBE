package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.TelefonoDireccion;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TelefonoDireccion.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface TelefonoDireccionService extends EntityService<TelefonoDireccion>{
	 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  TelefonoDireccion selectById(Long id) throws Throwable;
	
}