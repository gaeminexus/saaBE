package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.ReferenciaFamiliar;

import jakarta.ejb.Local;

@Local
public interface ReferenciaFamiliarDaoService extends EntityDao<ReferenciaFamiliar> {

    /**
     * Retorna las referencias familiares de una entidad.
     * @param idEntidad código de la entidad
     * @return lista de referencias familiares
     */
    List<ReferenciaFamiliar> selectByParent(Long idEntidad);
}
