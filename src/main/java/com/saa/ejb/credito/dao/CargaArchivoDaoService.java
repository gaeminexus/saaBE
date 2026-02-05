package com.saa.ejb.credito.dao;

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

}
