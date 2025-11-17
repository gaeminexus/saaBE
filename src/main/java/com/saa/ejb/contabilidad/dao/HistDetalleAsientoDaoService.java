package com.saa.ejb.contabilidad.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.HistDetalleAsiento;

import jakarta.ejb.Remote;

@Remote
public interface HistDetalleAsientoDaoService  extends EntityDao<HistDetalleAsiento>  {
	
	/**
	 * Elimina los registros por codigo de mayorizacin
	 * @param idHistAsiento	: Codigo de asiento histórico
	 * @throws Throwable	: Excepcion
	 */
	void deleteByHistAsiento(Long idHistAsiento) throws Throwable;
	
	
}


