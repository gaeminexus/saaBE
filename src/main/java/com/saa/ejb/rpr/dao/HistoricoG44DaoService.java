package com.saa.ejb.rpr.dao;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.HistoricoG44;

import jakarta.ejb.Local;
@Local
public interface HistoricoG44DaoService extends EntityDao<HistoricoG44> {
    List<HistoricoG44> selectByIdentificacion(String identificacion);

    /**
     * Retorna los registros de HM44 cuya identificación existe en ENTD
     * pero con idEstado diferente a 30 (ex-jubilados).
     */
    List<HistoricoG44> selectExJubilados() throws Throwable;
    
    /**
     * Carga múltiples HistoricoG44 por identificaciones en una sola consulta.
     * Optimización para evitar N+1 queries.
     */
    List<HistoricoG44> selectByIdentificacionesIn(List<String> identificaciones) throws Throwable;
}
