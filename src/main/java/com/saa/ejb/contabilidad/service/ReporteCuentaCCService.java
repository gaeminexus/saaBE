package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.ReporteCuentaCC;

import jakarta.ejb.Remote;

@Remote
public interface ReporteCuentaCCService extends EntityService<ReporteCuentaCC> {
	
	 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	 ReporteCuentaCC selectById(Long id) throws Throwable;

}
