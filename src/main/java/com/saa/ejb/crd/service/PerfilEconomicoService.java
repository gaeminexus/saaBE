package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.PerfilEconomico;

import jakarta.ejb.Local;

@Local
public interface PerfilEconomicoService extends EntityService<PerfilEconomico> {

    /**
     * Para G45 — Retorna el perfil económico de una entidad.
     * De aquí se obtiene patrimonioNeto → patrimonio y origenOtrosIngresos → origenIngresos.
     */
    List<PerfilEconomico> selectByEntidad(Long codigoEntidad) throws Throwable;
}

