package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Conciliacion;
import com.saa.model.tsr.DetalleConciliacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DetalleConciliacion.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface DetalleConciliacionService extends EntityService<DetalleConciliacion>{
	
	  
	  /**
	   * Inserta el detalle de la conciliacion bancaria
	 * @param conciliacion	: Conciliacion sobre la que se genera el detalle
	 * @param mes			: Mes a conciliar
	 * @param anio			: Anio a conciliar
	 * @throws Throwable	: Excepcion
	 */
	void insertaDetalleConciliacion(Conciliacion conciliacion, Long mes, Long anio) throws Throwable;
	
	/**
	 * Eliminar todos los registros filtrados por idConciliacion
	 * @param idConciliacion: Id de la conciliacion 
	 * @throws Throwable: Excepcions
	 */
	void deleteByIdConciliacion(Long idConciliacion) throws Throwable;
	
}
