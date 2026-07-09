package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.HistoricoCPRM;

import jakarta.ejb.Local;

@Local
public interface HistoricoCPRMDaoService extends EntityDao<HistoricoCPRM> {

    /**
     * Busca un registro histórico por identificación.
     * @param identificacion Identificación del partícipe
     * @return Lista de registros históricos encontrados
     */
    List<HistoricoCPRM> selectByIdentificacion(String identificacion) throws Throwable;
}
