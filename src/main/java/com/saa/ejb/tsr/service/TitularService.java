package com.saa.ejb.tsr.service;
import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Titular;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Persona.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface TitularService extends EntityService<Titular>{
 	 
	/**
	 * Valida si la identificacion(Cedula o Ruc) es correcta
	 * @param identificacion: Identificacion a validar
	 * @return				: 1= OK  0 = MAL INGRESADO
	 * @throws Throwable	: Excepcion
	 */
	int validaIdentificacion(String identificacion) throws Throwable;
	
}
