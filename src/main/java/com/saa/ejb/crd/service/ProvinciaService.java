package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Provincia;

import jakarta.ejb.Local;

@Local
public interface ProvinciaService extends EntityService<Provincia> {
    /**
     * Busca una Provincia por nombre (comparación sin distinción de mayúsculas).
     *
     * @param nombre Nombre de la provincia (ej. "PICHINCHA")
     * @return Provincia encontrada o null
     */
    Provincia selectByNombre(String nombre) throws Throwable;
}