package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.HistoricoCJBM;
import jakarta.ejb.Local;

@Local
public interface HistoricoCJBMService extends EntityService<HistoricoCJBM> {

    /**
     * Busca un registro histórico por identificación.
     */
    List<HistoricoCJBM> selectByIdentificacion(String identificacion) throws Throwable;

    /**
     * Carga múltiples HistoricoCJBM por identificaciones en una sola consulta.
     * Optimización para evitar N+1 queries.
     */
    List<HistoricoCJBM> selectByIdentificacionesIn(List<String> identificaciones) throws Throwable;
}
