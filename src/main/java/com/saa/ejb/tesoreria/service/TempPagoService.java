package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.TempPago;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad TempPago.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface TempPagoService extends EntityService<TempPago>{

	/**
	 * Elimina datos de las tablas temporales
	 * @param idUsuario	: Id de usuario
	 * @throws Throwable: Excepcion
	 */
	void eliminarPagosTemporales(Long idUsuario)throws Throwable;

}