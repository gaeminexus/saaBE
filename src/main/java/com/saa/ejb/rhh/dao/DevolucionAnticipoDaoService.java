package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.DevolucionAnticipo;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService DevolucionAnticipo.
 */
@Local
public interface DevolucionAnticipoDaoService extends EntityDao<DevolucionAnticipo> {

	/**
	 * Devoluciones de un anticipo, más recientes primero.
	 *
	 * @param idAnticipo	: Id del anticipo
	 * @return				: Las devoluciones del anticipo, o lista vacia
	 * @throws Throwable	: Excepcion
	 */
	List<DevolucionAnticipo> selectByAnticipo(Long idAnticipo) throws Throwable;

}
