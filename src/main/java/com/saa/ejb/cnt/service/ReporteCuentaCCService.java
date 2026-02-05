package com.saa.ejb.cnt.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.ReporteCuentaCC;

import jakarta.ejb.Local;

@Local
public interface ReporteCuentaCCService extends EntityService<ReporteCuentaCC> {
	
	 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	 ReporteCuentaCC selectById(Long id) throws Throwable;

}
