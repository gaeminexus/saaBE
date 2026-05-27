package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.SaldoOperacionG48;
import jakarta.ejb.Local;

@Local
public interface SaldoOperacionG48DaoService extends EntityDao<SaldoOperacionG48> {

    /** Retorna todos los registros G48 de un EJRD específico. */
    List<SaldoOperacionG48> selectByDetalle(Long codigoDetalle) throws Throwable;

    /** Retorna el registro G48 de un EJRD específico filtrado por numeroOperacion. */
    SaldoOperacionG48 selectByDetalleYOperacion(Long codigoDetalle, String numeroOperacion) throws Throwable;
}
