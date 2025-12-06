package com.saa.ejb.credito.dao;

import java.math.BigDecimal;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.credito.Entidad;

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

}
