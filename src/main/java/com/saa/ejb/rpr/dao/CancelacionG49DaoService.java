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
}
