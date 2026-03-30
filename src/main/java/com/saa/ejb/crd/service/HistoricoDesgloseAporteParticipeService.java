package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.HistoricoDesgloseAporteParticipe;

import jakarta.ejb.Local;

@Local
public interface HistoricoDesgloseAporteParticipeService extends EntityService<HistoricoDesgloseAporteParticipe> {
	
	/**
	 * Obtiene todos los registros de desglose por ID de carga
	 * 
	 * @param idCarga ID de la carga
	 * @return Lista de registros encontrados
	 * @throws Throwable Si ocurre algún error
	 */
	List<HistoricoDesgloseAporteParticipe> obtenerPorCarga(Long idCarga) throws Throwable;
	
	/**
	 * Obtiene registros de desglose por cédula del partícipe
	 * 
	 * @param cedula Número de cédula
	 * @return Lista de registros encontrados
	 * @throws Throwable Si ocurre algún error
	 */
	List<HistoricoDesgloseAporteParticipe> obtenerPorCedula(String cedula) throws Throwable;
	
	/**
	 * Obtiene un registro específico por código interno y carga
	 * 
	 * @param codigoInterno Código interno del partícipe en Petrocomercial
	 * @param idCarga ID de la carga
	 * @return Registro encontrado o null
	 * @throws Throwable Si ocurre algún error
	 */
	HistoricoDesgloseAporteParticipe obtenerPorCodigoInternoYCarga(String codigoInterno, Long idCarga) throws Throwable;
}
