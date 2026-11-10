package com.saa.ejb.cnt.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.RequerimientoNovedad;

import jakarta.ejb.Local;

/**
 * C25 — DAO de {@link RequerimientoNovedad} (CNT.RQNV): lecturas estándar (selectAll, selectById, selectByCriteria).
 * Las escrituras van por SeguimientoNovedadService, que valida estados, usuario e historial.
 */
@Local
public interface RequerimientoNovedadDaoService extends EntityDao<RequerimientoNovedad> {
}
