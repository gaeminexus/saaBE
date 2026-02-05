package com.saa.ejb.cnt.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.HistDetalleMayorizacion;

import jakarta.ejb.Local;

@Local
public interface HistDetalleMayorizacionDaoService  extends EntityDao<HistDetalleMayorizacion>  {

	/**
	 * Elimina los registros por codigo de mayorizacin
	 * @param idDesmayorizacion	: Codigo de desmayorizacion
	 * @throws Throwable		: Excepcion
	 */
	void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable; 
	
}
