package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.ReferenciaFamiliar;

import jakarta.ejb.Local;

@Local
public interface ReferenciaFamiliarService extends EntityService<ReferenciaFamiliar> {

    /**
     * Retorna las referencias familiares de una entidad.
     * @param idEntidad código de la entidad
     * @return lista de referencias familiares
     */
    List<ReferenciaFamiliar> selectByParent(Long idEntidad) throws Throwable;
}
