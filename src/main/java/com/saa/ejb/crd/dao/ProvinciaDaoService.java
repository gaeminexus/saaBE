package com.saa.ejb.crd.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Provincia;

import jakarta.ejb.Local;

@Local
public interface ProvinciaDaoService extends EntityDao<Provincia> {

    /**
     * Busca una Provincia cuyo nombre coincida (ignorando mayúsculas) con el valor dado.
     *
     * @param nombre Nombre de la provincia (ej. "PICHINCHA")
     * @return Provincia encontrada o null
     */
    Provincia selectByNombre(String nombre) throws Throwable;
}