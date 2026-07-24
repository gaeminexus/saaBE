package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.ReferenciaPersonal;

import jakarta.ejb.Local;

@Local
public interface ReferenciaPersonalDaoService extends EntityDao<ReferenciaPersonal> {

    /**
     * Retorna las referencias personales de una entidad.
     * @param idEntidad código de la entidad
     * @return lista de referencias personales
     */
    List<ReferenciaPersonal> selectByParent(Long idEntidad);
}
