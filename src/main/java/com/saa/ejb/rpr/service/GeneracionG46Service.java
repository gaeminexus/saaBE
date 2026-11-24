package com.saa.ejb.rpr.service;

import com.saa.model.crd.Prestamo;
import com.saa.model.rpr.DetalleEjecucionReporte;
import com.saa.model.rpr.NuevoPrestamoG46;

import jakarta.ejb.Local;

@Local
public interface GeneracionG46Service {
    long generar(DetalleEjecucionReporte detalle) throws Throwable;

    /**
     * C40 (23-nov-2026): la fila de la G46 de un prestamo, sin grabarla. Es la misma que arma {@link #generar}; la usa
     * la sustitucion AGREGAR para dar de alta un prestamo fuera de su mes de concesion.
     */
    NuevoPrestamoG46 mapear(Prestamo prestamo, DetalleEjecucionReporte detalle) throws Throwable;
}
