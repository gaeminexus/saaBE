package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CargaArchivo;

import jakarta.ejb.Local;

@Local
public interface CargaArchivoDaoService extends EntityDao<CargaArchivo>{
	
	/** Recupera los archivos cargados en un año determinado
	 * @param :anio
	 * @return Lista de CargaArchivo
	 */
	List<CargaArchivo> selectByAnio(Long anio) throws Throwable;
	
	/**
	 * Busca todas las cargas con estado específico (estado 3 = procesada)
	 * @param estado Estado de la carga
	 * @return Lista de CargaArchivo encontradas
	 * @throws Throwable Si ocurre un error
	 */
	List<CargaArchivo> selectByEstado(Long estado) throws Throwable;

}
