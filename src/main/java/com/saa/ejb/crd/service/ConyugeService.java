package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.Conyuge;

import jakarta.ejb.Local;

@Local
public interface ConyugeService extends EntityService<Conyuge> {

    /**
     * Retorna el cónyuge asociado a una entidad.
     * @param idEntidad código de la entidad
     * @return lista (máximo 1 registro)
     */
    List<Conyuge> selectByParent(Long idEntidad) throws Throwable;
}
