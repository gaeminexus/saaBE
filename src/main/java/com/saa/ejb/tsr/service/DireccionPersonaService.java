package com.saa.ejb.tsr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.DireccionPersona;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DireccionPersona.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface DireccionPersonaService extends EntityService<DireccionPersona>{
	
}
