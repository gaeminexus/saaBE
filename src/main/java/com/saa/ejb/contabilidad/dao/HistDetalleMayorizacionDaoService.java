package com.saa.ejb.contabilidad.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.HistDetalleMayorizacion;

import jakarta.ejb.Remote;

@Remote
public interface HistDetalleMayorizacionDaoService  extends EntityDao<HistDetalleMayorizacion>  {

	/**
	 * Elimina los registros por codigo de mayorizacin
	 * @param idDesmayorizacion	: Codigo de desmayorizacion
	 * @throws Throwable		: Excepcion
	 */
	void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable; 
	
}
