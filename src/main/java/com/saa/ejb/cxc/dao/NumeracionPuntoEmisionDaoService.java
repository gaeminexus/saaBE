package com.saa.ejb.cxc.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.NumeracionPuntoEmision;

import jakarta.ejb.Local;

/**
 * Interface DAO para la entidad NumeracionPuntoEmision
 */
@Local
public interface NumeracionPuntoEmisionDaoService extends EntityDao<NumeracionPuntoEmision> {
	
	/**
	 * Busca numeraciones por punto de emisión
	 * @param idPuntoEmision ID del punto de emisión
	 * @return Lista de numeraciones
	 * @throws Throwable Excepción
	 */
	List<NumeracionPuntoEmision> selectByPuntoEmision(Long idPuntoEmision) throws Throwable;
	
	/**
	 * Busca numeración por punto de emisión y tipo de documento
	 * @param idPuntoEmision ID del punto de emisión
	 * @param tipoDoc Tipo de documento
	 * @return Numeración encontrada
	 * @throws Throwable Excepción
	 */
	NumeracionPuntoEmision selectByPuntoEmisionTipo(Long idPuntoEmision, String tipoDoc) throws Throwable;
}
