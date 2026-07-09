package com.saa.ejb.cxc.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.PuntoEmision;

import jakarta.ejb.Local;

/**
 * Interface DAO para la entidad PuntoEmision
 */
@Local
public interface PuntoEmisionDaoService extends EntityDao<PuntoEmision> {
	
	/**
	 * Busca puntos de emisión por establecimiento
	 * @param idEstablecimiento ID del establecimiento
	 * @return Lista de puntos de emisión
	 * @throws Throwable Excepción
	 */
	List<PuntoEmision> selectByEstablecimiento(Long idEstablecimiento) throws Throwable;
}
