package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.rpr.HistoricoG48;
import jakarta.ejb.Local;

@Local
public interface HistoricoG48Service extends EntityService<HistoricoG48> {

    /** Busca un registro en HM48 por el número de operación (PK). Retorna null si no existe. */
    HistoricoG48 selectByNumeroOperacion(String numeroOperacion) throws Throwable;

    /**
     * Retorna los registros de HM48 cuyo numeroOperacion NO existe en CG48
     * del período de junio 2025. Usado para el Grupo 3 del G49 de junio 2025.
     */
    List<HistoricoG48> selectEnHm48NoEnCg48Junio2025() throws Throwable;
}
