package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Conciliacion;
import com.saa.model.tsr.HistConciliacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad HistConciliacion.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface HistConciliacionService extends EntityService<HistConciliacion>{
	
	  /**
	   * Respalda una conciliacion en la entidad de históricos
	 * @param conciliacion	: Conciliacion a respaldar
	 * @throws Throwable	: Excepcion
	 */
	void copiaConciliacion(Conciliacion conciliacion) throws Throwable;
	
}
