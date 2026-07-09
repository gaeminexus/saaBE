package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.SaldoOperacionG48;

import jakarta.ejb.Local;

@Local
public interface SaldoOperacionG48Service extends EntityService<SaldoOperacionG48> {

    List<SaldoOperacionG48> selectByDetalle(Long codigoDetalle) throws Throwable;

    SaldoOperacionG48 selectByDetalleYOperacion(Long codigoDetalle, String numeroOperacion) throws Throwable;
}
