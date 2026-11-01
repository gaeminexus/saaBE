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
     * Retorna los registros de HM48 cuyo numeroOperacion NO existe en la CG48 del detalle G48
     * indicado (el de la ejecución que se genera; D-044). Usado en la G49 de junio 2025, Grupo 3.
     */
    List<HistoricoG48> selectEnHm48NoEnCg48(Long codigoDetalleG48) throws Throwable;
}
