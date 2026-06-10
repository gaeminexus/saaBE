package com.saa.ejb.rpr.service;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.HistoricoG44;
import jakarta.ejb.Local;
@Local
public interface HistoricoG44Service extends EntityDao<HistoricoG44> {
    List<HistoricoG44> selectByIdentificacion(String identificacion);
    List<HistoricoG44> selectExJubilados() throws Throwable;
    
    /**
     * Carga múltiples HistoricoG44 por identificaciones en una sola consulta.
     * Optimización para evitar N+1 queries.
     */
    List<HistoricoG44> selectByIdentificacionesIn(List<String> identificaciones) throws Throwable;
}
