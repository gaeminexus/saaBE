package com.saa.ejb.cnt.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.NovedadSeguimiento;

import jakarta.ejb.Local;

/**
 * C25 — DAO de {@link NovedadSeguimiento} (CNT.NVDD): lecturas estándar (selectAll, selectById, selectByCriteria).
 * Las escrituras van por SeguimientoNovedadService, que valida estados, usuario e historial.
 */
@Local
public interface NovedadSeguimientoDaoService extends EntityDao<NovedadSeguimiento> {
}
