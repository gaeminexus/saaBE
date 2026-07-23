package com.saa.ejb.cxp.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cxp.DetalleRetencionCompraV2;

import jakarta.ejb.Local;

/**
 * Interface Service para la entidad DetalleRetencionCompraV2 (PGS.DRC2).
 */
@Local
public interface DetalleRetencionCompraV2Service extends EntityService<DetalleRetencionCompraV2> {

	/**
	 * Recupera un DetalleRetencionCompraV2 por su ID.
	 * @param id ID del detalle
	 * @return DetalleRetencionCompraV2 encontrado
	 * @throws Throwable Excepción
	 */
	DetalleRetencionCompraV2 selectById(Long id) throws Throwable;

	/**
	 * Recupera los detalles de una Retención Compra V2 por su ID.
	 * @param idRetencionCompraV2 ID de la RetencionCompraV2
	 * @return Lista de DetalleRetencionCompraV2
	 * @throws Throwable Excepción
	 */
	List<DetalleRetencionCompraV2> selectByRetencionCompraV2(Long idRetencionCompraV2) throws Throwable;
}
