/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.HistMayorizacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad HistMayorizacion.
 */
@Local
public interface HistMayorizacionDaoService extends EntityDao<HistMayorizacion> {
	
	/**
	 * Elimina los registros por codigo de desmayorizacion
	 * @param idDesmayorizacion	: Codigo de desmayorizacion
	 * @throws Throwable		: Excepcion
	 */
	void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable; 
	
	/**
	 * Recupera por codigo de mayorizacion
	 * @param idMayorizacion	: Id de mayorizacion
	 * @return					: Registro de historico de mayorizacion
	 * @throws Throwable		: Excepcion
	 */
	HistMayorizacion selectByMayorizacion(Long idMayorizacion) throws Throwable;
	
}
