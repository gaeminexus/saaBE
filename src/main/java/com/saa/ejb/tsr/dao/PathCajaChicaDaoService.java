package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.PathCajaChica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * Dao Service PathCajaChica.
 */
@Local
public interface PathCajaChicaDaoService extends EntityDao<PathCajaChica> {

	/**
	 * Adjuntos de un movimiento de caja chica.
	 * @param idMovimiento : Id del movimiento
	 * @return             : Adjuntos del movimiento
	 * @throws Throwable   : Excepcion
	 */
	List<PathCajaChica> selectByMovimiento(Long idMovimiento) throws Throwable;

}
