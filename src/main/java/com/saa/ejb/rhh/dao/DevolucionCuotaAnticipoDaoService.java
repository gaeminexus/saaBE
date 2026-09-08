package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.DevolucionCuotaAnticipo;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DevolucionCuotaAnticipo.
 */
@Local
public interface DevolucionCuotaAnticipoDaoService extends EntityDao<DevolucionCuotaAnticipo> {

	/**
	 * Filas de una devolución, en el orden en que se aplicaron.
	 *
	 * @param idDevolucion	: Id de la devolución
	 * @return				: Las filas de la devolución, o lista vacia
	 * @throws Throwable	: Excepcion
	 */
	List<DevolucionCuotaAnticipo> selectByDevolucion(Long idDevolucion) throws Throwable;

}
