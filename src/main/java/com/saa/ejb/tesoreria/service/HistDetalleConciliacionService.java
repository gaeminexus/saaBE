package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.Conciliacion;
import com.saa.model.tesoreria.HistConciliacion;
import com.saa.model.tesoreria.HistDetalleConciliacion;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad HistDetalleConciliacion.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface HistDetalleConciliacionService extends EntityService<HistDetalleConciliacion>{
 	 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  HistDetalleConciliacion selectById(Long id) throws Throwable;
	  
	 /**
	  * Respalda el detalle de una conciliacion
	 * @param original	: Conciliacion que origina el respaldo
	 * @param respaldoCabecera	: Cabecera del respaldo 
	 * @throws Throwable: Excepcion
	 */
	void respaldaDetalleConciliacion(Conciliacion original, HistConciliacion respaldoCabecera) throws Throwable;
	
}