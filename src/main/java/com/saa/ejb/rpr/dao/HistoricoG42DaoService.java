package com.saa.ejb.rpr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rpr.HistoricoG42;

import jakarta.ejb.Local;

@Local
public interface HistoricoG42DaoService extends EntityDao<HistoricoG42> {

    /**
     * Retorna los registros del HistoricoG42 cuya identificacion NO aparece
     * en el G42 del mes actual. Comparación con NOT EXISTS en BD.
     *
     * Usado en G43 — Camino B: cuando no existe EJRC del mes anterior (primera ejecución).
     *
     * @param codigoDetalleActual Código del EJRD G42 del mes actual
     * @return Lista de HistoricoG42 que ya no tienen saldo este mes
     */
    List<HistoricoG42> selectCesantesDesdeHistorico(Long codigoDetalleActual) throws Throwable;
}
