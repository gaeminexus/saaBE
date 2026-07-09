package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Participe;

import jakarta.ejb.Local;

@Local
public interface ParticipeDaoService extends EntityDao<Participe> {

    /**
     * Recupera el partícipe asociado a una entidad específica.
     * @param codigoEntidad : Código de la entidad
     * @return : Listado de partícipes (normalmente 1)
     * @throws Throwable : Excepcion
     */
    List<Participe> selectByEntidad(Long codigoEntidad) throws Throwable;
}
