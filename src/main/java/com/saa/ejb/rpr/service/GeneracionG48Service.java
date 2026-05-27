package com.saa.ejb.rpr.service;

import com.saa.model.rpr.DetalleEjecucionReporte;
import jakarta.ejb.Local;

@Local
public interface GeneracionG48Service {
    long generar(DetalleEjecucionReporte detalle) throws Throwable;
}
