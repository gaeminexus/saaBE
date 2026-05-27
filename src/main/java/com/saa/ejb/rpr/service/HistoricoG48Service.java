package com.saa.ejb.rpr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.HistoricoG48;
import jakarta.ejb.Local;

@Local
public interface HistoricoG48Service extends EntityService<HistoricoG48> {

    /** Busca un registro en HM48 por el número de operación (PK). Retorna null si no existe. */
    HistoricoG48 selectByNumeroOperacion(String numeroOperacion) throws Throwable;
}
