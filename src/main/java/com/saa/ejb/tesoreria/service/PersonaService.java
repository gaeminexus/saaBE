package com.saa.ejb.tesoreria.service;
import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.Persona;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Persona.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface PersonaService extends EntityService<Persona>{
 	 
	/**
	 * Valida si la identificacion(Cedula o Ruc) es correcta
	 * @param identificacion: Identificacion a validar
	 * @return				: 1= OK  0 = MAL INGRESADO
	 * @throws Throwable	: Excepcion
	 */
	int validaIdentificacion(String identificacion) throws Throwable;
	
}