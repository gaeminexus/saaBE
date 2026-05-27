package com.saa.ejb.rpr.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.HistoricoG48;
import jakarta.ejb.Local;
@Local
public interface HistoricoG48DaoService extends EntityDao<HistoricoG48> {

    /** Busca en HM48 por numeroOperacion (PK). Retorna null si no existe. */
    HistoricoG48 selectByNumeroOperacion(String numeroOperacion) throws Throwable;
}
