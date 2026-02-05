package com.saa.ejb.tsr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.CajaFisica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CajaFisica.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface CajaFisicaService extends EntityService<CajaFisica>{
	
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  CajaFisica selectById(Long id) throws Throwable;
	
}
