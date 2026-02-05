package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.TempCobro;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempCobroCheque.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface TempCobroService extends EntityService<TempCobro>{
 	 
	/**
	 * Elimina Todos los Cobros de Usuario por Caja
	 * @param idUsuarioCaja	: Id Usuario por Caja
	 * @throws Throwable	: Excepcions
	 */
	 void eliminaCobroByIdUsuarioCaja (Long idUsuarioCaja) throws Throwable;
	
	/**
	 * Elimina los datos temporales de un cobro generado
	 * @param idCobro	: Id del cobro
	 * @throws Throwable: Excepcion
	 */
	 void eliminarDatosTemporales(Long idCobro) throws Throwable;
}
