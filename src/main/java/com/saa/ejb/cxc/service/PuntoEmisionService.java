package com.saa.ejb.cxc.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cxc.PuntoEmision;

import jakarta.ejb.Local;

/**
 * Interface de servicio para la entidad PuntoEmision
 */
@Local
public interface PuntoEmisionService extends EntityService<PuntoEmision> {
	
	/**
	 * Recupera entidad por ID
	 * @param id ID del punto de emisión
	 * @return PuntoEmision encontrado
	 * @throws Throwable Excepción
	 */
	PuntoEmision selectById(Long id) throws Throwable;
	
	/**
	 * Guarda un punto de emisión
	 * @param puntoEmision Punto de emisión a guardar
	 * @return Punto de emisión guardado
	 * @throws Throwable Excepción
	 */
	PuntoEmision saveSingle(PuntoEmision puntoEmision) throws Throwable;
	
	/**
	 * Busca puntos de emisión por establecimiento
	 * @param idEstablecimiento ID del establecimiento
	 * @return Lista de puntos de emisión
	 * @throws Throwable Excepción
	 */
	List<PuntoEmision> selectByEstablecimiento(Long idEstablecimiento) throws Throwable;
}
