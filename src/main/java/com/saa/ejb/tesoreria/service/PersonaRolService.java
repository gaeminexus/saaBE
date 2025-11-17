package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.PersonaRol;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad PersonaRol.
 *  Accede a los metodos DAO y procesa los datos.</p>
 */
@Remote
public interface PersonaRolService extends EntityService<PersonaRol>{
	
}