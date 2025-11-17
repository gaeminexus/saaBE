package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.DireccionPersona;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DireccionPersona.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface DireccionPersonaService extends EntityService<DireccionPersona>{
	
}
