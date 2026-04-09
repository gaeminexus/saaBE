package com.saa.ejb.crd.dao;

import java.math.BigDecimal;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Entidad;

import jakarta.ejb.Local;

@Local
public interface EntidadDaoService extends EntityDao<Entidad> {
	
	/**
	 * Selecciona los ParticipeXCargaArchivo por codigoPetro.
	 * @param codigoPetro: Código Petro a buscar.
	 * @return: Lista de ParticipeXCargaArchivo asociados al código Petro.
	 * @throws Throwable: Excepción en caso de error.
	 */
	List<Entidad> selectByCodigoPetro(Long codigoPetro) throws Throwable;
	
	/**
	 * Selecciona las coincidencias de ParticipeXCargaArchivo por nombre.
	 * @param nombre: Nombre a buscar.
	 * @return: Lista de ParticipeXCargaArchivo que coinciden con el nombre.
	 * @throws Throwable: Excepción en caso de error.
	 */
	List<BigDecimal> selectCoincidenciasByNombre(String nombre) throws Throwable;
	
	/**
	 * Recupera una lista de Entidad cuyo nombre completo coincida con el proporcionado por petro de 35 caracteres.
	 * @param nombre: Nombre completo a buscar.
	 * @return: Lista de Entidad que coinciden con el nombre completo.
	 * @throws Throwable: Excepción en caso de error.
	 */
	List<Entidad> selectByNombrePetro35(String nombre) throws Throwable;
	
	/**
	 * Obtiene resumen de entidades agrupadas por estado (para dashboard)
	 * 
	 * @param estadosPermitidos Lista de estados a incluir (ej: 10, 2, 30)
	 * @return Lista de resúmenes con estado y cantidad de entidades
	 * @throws Throwable Excepción en caso de error
	 */
	java.util.List<com.saa.model.crd.dto.EntidadResumenEstadoDTO> selectResumenPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable;
	
	/**
	 * Obtiene resumen de préstamos agrupados por estado de entidad
	 * 
	 * @param estadosPermitidos Lista de estados a incluir (ej: 10, 2, 30)
	 * @return Lista de resúmenes con estado y total de préstamos
	 * @throws Throwable Excepción en caso de error
	 */
	java.util.List<com.saa.model.crd.dto.EntidadResumenPrestamosDTO> selectResumenPrestamosPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable;
	
	/**
	 * Obtiene resumen de aportes agrupados por estado de entidad
	 * 
	 * @param estadosPermitidos Lista de estados a incluir (ej: 10, 2, 30)
	 * @return Lista de resúmenes con estado y total de aportes
	 * @throws Throwable Excepción en caso de error
	 */
	java.util.List<com.saa.model.crd.dto.EntidadResumenAportesDTO> selectResumenAportesPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable;
	
	/**
	 * Obtiene resumen consolidado (entidades, préstamos y aportes) por estado
	 * 
	 * @param estadosPermitidos Lista de estados a incluir (ej: 10, 2, 30)
	 * @return Lista de resúmenes consolidados
	 * @throws Throwable Excepción en caso de error
	 */
	java.util.List<com.saa.model.crd.dto.EntidadResumenConsolidadoDTO> selectResumenConsolidadoPorEstado(
			java.util.List<Long> estadosPermitidos) throws Throwable;

}
