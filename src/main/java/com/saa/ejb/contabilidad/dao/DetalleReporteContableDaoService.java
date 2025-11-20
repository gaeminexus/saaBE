/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.DetalleReporteContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad DetalleReporteContable.
 */
@Local
public interface DetalleReporteContableDaoService extends EntityDao<DetalleReporteContable> {

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
