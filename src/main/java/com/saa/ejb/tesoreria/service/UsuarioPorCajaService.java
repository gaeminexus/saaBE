package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.UsuarioPorCaja;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad UsuarioPorCaja.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface UsuarioPorCajaService extends EntityService<UsuarioPorCaja>{
	
}
