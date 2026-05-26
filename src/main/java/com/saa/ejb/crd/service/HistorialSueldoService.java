package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.HistorialSueldo;

import jakarta.ejb.Local;

@Local
public interface HistorialSueldoService extends EntityService<HistorialSueldo>{

    /**
     * G42 — Aporte Personal: SUM(montoCesantia + montoJubilacion) por entidad
     * con estado = 99. Retorna Object[]{Long codigoEntidad, Double suma}
     */
    List<Object[]> selectSumaAportePersonalPorEntidad(java.time.LocalDateTime fechaCorte) throws Throwable;

}
