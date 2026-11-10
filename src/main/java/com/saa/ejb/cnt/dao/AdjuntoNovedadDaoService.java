package com.saa.ejb.cnt.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.AdjuntoNovedad;

import jakarta.ejb.Local;

/**
 * C25 — DAO de {@link AdjuntoNovedad} (CNT.ADNV): lecturas estándar (selectAll, selectById, selectByCriteria).
 * Las escrituras van por SeguimientoNovedadService, que valida estados, usuario e historial.
 */
@Local
public interface AdjuntoNovedadDaoService extends EntityDao<AdjuntoNovedad> {
}
