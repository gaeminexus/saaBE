package com.saa.ejb.cxc.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.DetalleRetencionV2;

import jakarta.ejb.Local;

/**
 * Interface DAO para la entidad DetalleRetencionV2 (CBR.DRV2).
 */
@Local
public interface DetalleRetencionV2DaoService extends EntityDao<DetalleRetencionV2> {

	/**
	 * Recupera los detalles de una Retención V2 por su ID.
	 * @param idRetencionV2 ID de la RetencionV2
	 * @return Lista de DetalleRetencionV2
	 * @throws Throwable Excepción
	 */
	List<DetalleRetencionV2> selectByRetencionV2(Long idRetencionV2) throws Throwable;
}
