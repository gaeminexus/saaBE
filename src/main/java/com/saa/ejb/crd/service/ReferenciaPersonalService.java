package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.ReferenciaPersonal;

import jakarta.ejb.Local;

@Local
public interface ReferenciaPersonalService extends EntityService<ReferenciaPersonal> {

    /**
     * Retorna las referencias personales de una entidad.
     * @param idEntidad código de la entidad
     * @return lista de referencias personales
     */
    List<ReferenciaPersonal> selectByParent(Long idEntidad) throws Throwable;
}
