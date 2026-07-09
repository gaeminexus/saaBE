package com.saa.ejb.rpr.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.HistoricoCCPM;

import jakarta.ejb.Local;

@Local
public interface HistoricoCCPMDaoService extends EntityDao<HistoricoCCPM> {

    /**
     * Busca un registro histórico por número de operación.
     * @param numeroOperacion Número de operación
     * @return Registro histórico encontrado o null
     */
    HistoricoCCPM selectByNumeroOperacion(String numeroOperacion) throws Throwable;
}
