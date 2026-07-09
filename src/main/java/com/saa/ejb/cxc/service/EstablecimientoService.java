package com.saa.ejb.cxc.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cxc.Establecimiento;

import jakarta.ejb.Local;

/**
 * Interface de servicio para la entidad Establecimiento
 */
@Local
public interface EstablecimientoService extends EntityService<Establecimiento> {
	
	/**
	 * Recupera entidad por ID
	 * @param id ID del establecimiento
	 * @return Establecimiento encontrado
	 * @throws Throwable Excepción
	 */
	Establecimiento selectById(Long id) throws Throwable;
	
	/**
	 * Guarda un establecimiento
	 * @param establecimiento Establecimiento a guardar
	 * @return Establecimiento guardado
	 * @throws Throwable Excepción
	 */
	Establecimiento saveSingle(Establecimiento establecimiento) throws Throwable;
	
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
