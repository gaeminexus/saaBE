package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Conyuge;

import jakarta.ejb.Local;

@Local
public interface ConyugeDaoService extends EntityDao<Conyuge> {

    /**
     * Retorna el cónyuge asociado a una entidad.
     * @param idEntidad código de la entidad
     * @return lista (máximo 1 registro)
     */
    List<Conyuge> selectByParent(Long idEntidad);
}
