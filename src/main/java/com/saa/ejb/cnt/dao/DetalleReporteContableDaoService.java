/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.cnt.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.DetalleReporteContable;

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

	/**
	 * C19b/C38: filas de DTRP de un reporte por su codigo (RPRTCDGO). En un reporte de renglones propios cada fila es
	 * una regla cuenta -> renglon; las vigencias se filtran en el servicio.
	 */
	List<DetalleReporteContable> selectByReporte(Long codigoReporte) throws Throwable;
	
	
}
