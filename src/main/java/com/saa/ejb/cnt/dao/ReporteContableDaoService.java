/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.cnt.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.ReporteContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad ReporteContable.
 */
@Local
public interface ReporteContableDaoService  extends EntityDao<ReporteContable>  {

	/** Reporte (tipo de reporte del combo) por empresa y codigo alterno; null si no existe. */
	ReporteContable selectByEmpresaAlterno(Long empresa, Long codigoAlterno) throws Throwable;

	/** Reporte por empresa y codigo de estructura ante el ente de control (B17); null si no existe. */
	ReporteContable selectByEmpresaEstructura(Long empresa, String codigoEstructura) throws Throwable;
}
