package com.saa.ejb.cnt.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.ReporteContable;

import jakarta.ejb.Local;


@Local
public interface ReporteContableService extends EntityService<ReporteContable> {
	
 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	 ReporteContable selectById(Long id) throws Throwable;	 
	 
	 /**
	  * Valida que todas las cuentas incluidas en el reporte manejen centro de costo
	 * @param empresa		: Codigo de la empresa
	 * @param codigoAlterno	: Codigo alterno del reporte
	 * @throws Throwable	: Excepcion
	 */
	void validaCentroCostoEnReporte(Long empresa, Long codigoAlterno) throws Throwable;
	
}
