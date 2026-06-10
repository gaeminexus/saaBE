package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.HistoricoCPRM;
import jakarta.ejb.Local;

@Local
public interface HistoricoCPRMService extends EntityService<HistoricoCPRM> {

    /**
     * Busca un registro histórico por identificación.
     */
    List<HistoricoCPRM> selectByIdentificacion(String identificacion) throws Throwable;
}
