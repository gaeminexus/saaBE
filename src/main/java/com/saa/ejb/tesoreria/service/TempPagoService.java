package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.TempPago;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempPago.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface TempPagoService extends EntityService<TempPago>{

	/**
	 * Elimina datos de las tablas temporales
	 * @param idUsuario	: Id de usuario
	 * @throws Throwable: Excepcion
	 */
	void eliminarPagosTemporales(Long idUsuario)throws Throwable;

}
