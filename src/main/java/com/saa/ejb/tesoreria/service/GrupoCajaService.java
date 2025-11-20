package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.GrupoCaja;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad GrupoCaja.
 *  Accede a los metodos DAO y procesa los datos para el cliente</p>
 */
@Local
public interface GrupoCajaService extends EntityService<GrupoCaja>{
	
}
