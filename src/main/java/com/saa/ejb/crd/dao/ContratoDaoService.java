package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Contrato;

import jakarta.ejb.Local;

@Local
public interface ContratoDaoService extends EntityDao<Contrato> {

    /**
     * Todos los contratos de una entidad (activos e inactivos), más reciente primero.
     *
     * @param idEntidad  : Código de la entidad (partícipe)
     * @return           : Contratos de la entidad
     * @throws Throwable : Excepcion
     */
    List<Contrato> selectByEntidad(Long idEntidad) throws Throwable;

    /**
     * El contrato ACTIVO (CNTRESTD = 1) más reciente de una entidad. Null si no tiene
     * ninguno activo.
     *
     * @param idEntidad  : Código de la entidad (partícipe)
     * @return           : El contrato activo, o null
     * @throws Throwable : Excepcion
     */
    Contrato selectActivoPorEntidad(Long idEntidad) throws Throwable;

}
