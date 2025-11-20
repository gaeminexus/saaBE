/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.HistAsiento;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad HistAsiento.
 */
@Local
public interface HistAsientoDaoService extends EntityDao<HistAsiento> {

	/**
	 * Elimina los registros por codigo de mayorizacin
	 * @param idDesmayorizacion	: Codigo de desmayorizacion
	 * @throws Throwable		: Excepcion
	 */
	void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable;	
	
	/**
	 * Recupera un asiento de respaldo por el id del asiento origen
	 * @param idAsientoOrigen	: Id de asiento origen
	 * @param idDesmayorizacion	: Id de desmayorizacion origen
	 * @return					: Registro recuperado
	 * @throws Throwable		: Excepcion
	 */
	HistAsiento selectByIdAsientoOrigen(Long idAsientoOrigen, Long idDesmayorizacion) throws Throwable;
	
	/**
	 * Recupera los asientos historicos de una desmayorizacion
	 * @param idDesmayorizacion	: Id de desmayorizacion
	 * @return					: Asientos historicos de desmayorizacion
	 * @throws Throwable		: Excepcion
	 */
	List<HistAsiento> selectByDesmayorizacion(Long idDesmayorizacion) throws Throwable;
	
}
