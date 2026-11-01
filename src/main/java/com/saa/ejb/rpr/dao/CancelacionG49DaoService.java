package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.CancelacionG49;

import jakarta.ejb.Local;

@Local
public interface CancelacionG49DaoService extends EntityDao<CancelacionG49> {
    /**
     * Obtiene todos los registros de G49 por código de detalle de ejecución.
     */
    List<CancelacionG49> selectByDetalle(Long codigoDetalle) throws Throwable;

    /**
     * D-038 — fecha REAL de cancelación dentro del mes, por número de operación (PRSTIDAS):
     * {String numeroOperacion, Date fecha, String forma (tabla 39: O si se novó en el mes, N si no)}.
     * Solo operaciones con algún evento en el mes.
     */
    List<Object[]> selectCancelacionReal(java.time.LocalDate desde, java.time.LocalDate hasta) throws Throwable;
}
