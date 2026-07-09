package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Participe;

import jakarta.ejb.Local;

@Local
public interface ParticipeService extends EntityService<Participe> {

    /**
     * Recupera el partícipe asociado a una entidad específica.
     * @param codigoEntidad : Código de la entidad
     * @return : Listado de partícipes
     * @throws Throwable : Excepcion
     */
    List<Participe> selectByEntidad(Long codigoEntidad) throws Throwable;
}
