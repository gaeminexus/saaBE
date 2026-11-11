package com.saa.ejb.cnt.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.HistorialNovedad;

import jakarta.ejb.Local;

/**
 * C25 — DAO de {@link HistorialNovedad} (CNT.HSNV): lecturas estándar (selectAll, selectById, selectByCriteria).
 * Las escrituras van por SeguimientoNovedadService, que valida estados, usuario e historial.
 */
@Local
public interface HistorialNovedadDaoService extends EntityDao<HistorialNovedad> {
}
