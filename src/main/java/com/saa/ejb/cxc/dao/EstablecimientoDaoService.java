package com.saa.ejb.cxc.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.Establecimiento;

import jakarta.ejb.Local;

/**
 * Interface DAO para la entidad Establecimiento
 */
@Local
public interface EstablecimientoDaoService extends EntityDao<Establecimiento> {
	
	/**
	 * Busca establecimientos por facturador
	 * @param idFacturador ID del facturador
	 * @return Lista de establecimientos
	 * @throws Throwable Excepción
	 */
	List<Establecimiento> selectByFacturador(Long idFacturador) throws Throwable;
	
	/**
	 * Busca el establecimiento matriz de un facturador
	 * @param idFacturador ID del facturador
	 * @return Establecimiento matriz
	 * @throws Throwable Excepción
	 */
	Establecimiento selectMatriz(Long idFacturador) throws Throwable;
}
