package com.saa.ejb.cxc.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cxc.DetalleRetencionV2;

import jakarta.ejb.Local;

/**
 * Interface Service para la entidad DetalleRetencionV2 (CBR.DRV2).
 */
@Local
public interface DetalleRetencionV2Service extends EntityService<DetalleRetencionV2> {

	/**
	 * Recupera un DetalleRetencionV2 por su ID.
	 * @param id ID del detalle
	 * @return DetalleRetencionV2 encontrado
	 * @throws Throwable Excepción
	 */
	DetalleRetencionV2 selectById(Long id) throws Throwable;

	/**
	 * Recupera los detalles de una Retención V2 por su ID.
	 * @param idRetencionV2 ID de la RetencionV2
	 * @return Lista de DetalleRetencionV2
	 * @throws Throwable Excepción
	 */
	List<DetalleRetencionV2> selectByRetencionV2(Long idRetencionV2) throws Throwable;
}
