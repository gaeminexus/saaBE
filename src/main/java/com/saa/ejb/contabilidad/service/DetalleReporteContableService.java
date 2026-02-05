package com.saa.ejb.contabilidad.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.DetalleReporteContable;

import jakarta.ejb.Local;

@Local
public interface DetalleReporteContableService extends EntityService<DetalleReporteContable> {
	

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  DetalleReporteContable selectById(Long id) throws Throwable;	
	
	/**
	 * Recupera el detalle de acuerdo al codigo alterno del reporte, que se encuentre en estado activo 
	 * @param empresa		: Id de la Empresa
	 * @param codigoAlterno	: Codigo Alterno 
	 * @param estado		: Estado 
	 * @return				: Resultado
	 * @throws Throwable	: Excepcions
	 */
	List<DetalleReporteContable> selectByDetalleReporteContable (Long empresa, Long codigoAlterno)throws Throwable;	  

}
