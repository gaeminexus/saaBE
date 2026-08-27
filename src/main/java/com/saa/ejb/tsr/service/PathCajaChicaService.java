package com.saa.ejb.tsr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.PathCajaChica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad PathCajaChica: adjuntos (comprobantes
 * digitalizados) de un movimiento de caja chica.</p>
 */
@Local
public interface PathCajaChicaService extends EntityService<PathCajaChica> {

	/**
	 * Adjuntos de un movimiento de caja chica.
	 * @param idMovimiento : Id del movimiento
	 * @return             : Adjuntos del movimiento
	 * @throws Throwable   : Excepcion
	 */
	List<PathCajaChica> porMovimiento(Long idMovimiento) throws Throwable;

	/**
	 * Elimina un adjunto y su archivo físico.
	 * @param idPath    : Id del adjunto (TSR.PTCH)
	 * @throws Throwable : Excepcion
	 */
	void eliminar(Long idPath) throws Throwable;

}
