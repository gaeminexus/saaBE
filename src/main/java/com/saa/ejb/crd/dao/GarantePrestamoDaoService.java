package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.GarantePrestamo;

import jakarta.ejb.Local;

@Local
public interface GarantePrestamoDaoService extends EntityDao<GarantePrestamo> {

    /**
     * El garante ACTIVO ({@code GRPRESTD = 1}) de un préstamo; {@code null} si no tiene. El índice único de
     * {@code crd/sql/369} garantiza uno solo.
     */
    GarantePrestamo selectActivoByPrestamo(Long idPrestamo) throws Throwable;
}
