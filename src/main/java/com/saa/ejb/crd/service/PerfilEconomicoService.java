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

    /**
     * Igual que {@link #saveSingle(PerfilEconomico)}, pero además sella
     * {@code EntidadService.sellarActualizacion} sobre ENTD en la misma transacción
     * (pedido 9, pantalla de actualización de datos del partícipe).
     *
     * @param perfil  : PerfilEconomico a guardar
     * @param usuario : Usuario que hace el cambio (puede ser null)
     * @throws Throwable : Excepcion
     */
    PerfilEconomico saveSingle(PerfilEconomico perfil, String usuario) throws Throwable;
}

