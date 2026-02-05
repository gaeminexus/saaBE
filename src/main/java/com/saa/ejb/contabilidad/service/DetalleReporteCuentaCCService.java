package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.DetalleReporteCuentaCC;

import jakarta.ejb.Local;

@Local
public interface DetalleReporteCuentaCCService extends EntityService <DetalleReporteCuentaCC> {


	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  DetalleReporteCuentaCC selectById(Long id) throws Throwable;

}
