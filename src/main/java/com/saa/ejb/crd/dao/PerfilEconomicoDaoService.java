package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.PerfilEconomico;

import jakarta.ejb.Local;

@Local
public interface PerfilEconomicoDaoService extends EntityDao<PerfilEconomico> {

    /**
     * Para G45 — Retorna el perfil económico de una entidad.
     * De aquí se obtiene patrimonioNeto → patrimonio y origenOtrosIngresos → origenIngresos.
     *
     * @param codigoEntidad Código de la entidad
     * @return Lista de PerfilEconomico para esa entidad (puede ser vacía)
     */
    List<PerfilEconomico> selectByEntidad(Long codigoEntidad) throws Throwable;
}
