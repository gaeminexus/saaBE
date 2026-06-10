package com.saa.ejb.rpr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.HistoricoCCPM;
import jakarta.ejb.Local;

@Local
public interface HistoricoCCPMService extends EntityService<HistoricoCCPM> {

    /**
     * Busca un registro histórico por número de operación.
     */
    HistoricoCCPM selectByNumeroOperacion(String numeroOperacion) throws Throwable;
}
