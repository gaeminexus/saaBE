package com.saa.ejb.rpr.service;
import java.util.List;
import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.HistoricoG44;
import jakarta.ejb.Local;
@Local
public interface HistoricoG44Service extends EntityDao<HistoricoG44> {
    List<HistoricoG44> selectByIdentificacion(String identificacion);
}
