package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.UsuarioPorCaja;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad UsuarioPorCaja.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface UsuarioPorCajaService extends EntityService<UsuarioPorCaja>{
	
}
