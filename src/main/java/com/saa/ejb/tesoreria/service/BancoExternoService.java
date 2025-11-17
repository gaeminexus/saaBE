package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.BancoExterno;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad BancoExterno.
 *  Accede a los metodos DAO y procesa los datos para el cliente</p>
 */
@Remote
public interface BancoExternoService extends EntityService<BancoExterno>{
	
}