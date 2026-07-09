package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.HistoricoCJBM;

import jakarta.ejb.Local;

@Local
public interface HistoricoCJBMDaoService extends EntityDao<HistoricoCJBM> {

    /**
     * Busca un registro histórico por identificación.
     * @param identificacion Identificación del jubilado
     * @return Lista de registros históricos encontrados
     */
    List<HistoricoCJBM> selectByIdentificacion(String identificacion) throws Throwable;

    /**
     * Carga múltiples HistoricoCJBM por identificaciones en una sola consulta.
     * Optimización para evitar N+1 queries.
     * 
     * @param identificaciones Lista de identificaciones
     * @return Lista de HistoricoCJBM
     */
    List<HistoricoCJBM> selectByIdentificacionesIn(List<String> identificaciones) throws Throwable;
}
