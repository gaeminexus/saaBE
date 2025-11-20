/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.contabilidad.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.contabilidad.ReporteContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad ReporteContable.
 */
@Local
public interface ReporteContableDaoService  extends EntityDao<ReporteContable>  {
	 
}
