package com.saa.ejb.cxp.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.DetalleRetencionCompraV2;

import jakarta.ejb.Local;

/**
 * Interface DAO para la entidad DetalleRetencionCompraV2 (PGS.DRC2).
 */
@Local
public interface DetalleRetencionCompraV2DaoService extends EntityDao<DetalleRetencionCompraV2> {

	/**
	 * Recupera los detalles de una Retención Compra V2 por su ID.
	 * @param idRetencionCompraV2 ID de la RetencionCompraV2
	 * @return Lista de DetalleRetencionCompraV2
	 * @throws Throwable Excepción
	 */
	List<DetalleRetencionCompraV2> selectByRetencionCompraV2(Long idRetencionCompraV2) throws Throwable;
}
