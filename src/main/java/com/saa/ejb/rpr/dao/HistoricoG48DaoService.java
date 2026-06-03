package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.HistoricoG48;
import jakarta.ejb.Local;

@Local
public interface HistoricoG48DaoService extends EntityDao<HistoricoG48> {

    /** Busca en HM48 por numeroOperacion (PK). Retorna null si no existe. */
    HistoricoG48 selectByNumeroOperacion(String numeroOperacion) throws Throwable;

    /**
     * Retorna los registros de HM48 cuyo numeroOperacion NO existe en CG48
     * del período de junio 2025 (mes=6, anio=2025).
     * Usado exclusivamente en la ejecución del G49 de junio 2025 para el Grupo 3.
     */
    List<HistoricoG48> selectEnHm48NoEnCg48Junio2025() throws Throwable;
}
