package com.saa.ejb.rpr.serviceImpl;

import com.saa.ejb.rpr.service.GeneracionG50Service;
import com.saa.model.rpr.DetalleEjecucionReporte;

import jakarta.ejb.Stateless;

@Stateless
public class GeneracionG50ServiceImpl implements GeneracionG50Service {

    @Override
    public long generar(DetalleEjecucionReporte detalle) throws Throwable {
        System.out.println("G50 - Sin lógica implementada por el momento. Retorna 0 registros, OK.");
        return 0L;
    }
}
