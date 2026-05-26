package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Direccion;

import jakarta.ejb.Local;

@Local
public interface DireccionService extends EntityService<Direccion> {

    /**
     * Para G45 — Retorna las direcciones de una entidad.
     * De la primera dirección se obtiene parroquia.nombre → parroquia del G45.
     */
    List<Direccion> selectByParent(Long codigoEntidad) throws Throwable;
}

