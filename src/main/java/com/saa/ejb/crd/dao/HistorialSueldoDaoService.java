package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.HistorialSueldo;

import jakarta.ejb.Local;

@Local
public interface HistorialSueldoDaoService extends EntityDao<HistorialSueldo>  {

	/**
	 * Busca todos los registros de HistorialSueldo para una entidad específica
	 * 
	 * @param codigoEntidad Código de la entidad
	 * @return Lista de HistorialSueldo encontrados
	 * @throws Throwable Si ocurre algún error
	 */
	List<HistorialSueldo> selectByEntidad(Long codigoEntidad) throws Throwable;

}
