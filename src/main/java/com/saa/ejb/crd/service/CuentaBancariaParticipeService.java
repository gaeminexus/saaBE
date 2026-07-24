package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.CuentaBancariaParticipe;

import jakarta.ejb.Local;

@Local
public interface CuentaBancariaParticipeService extends EntityService<CuentaBancariaParticipe> {

    /**
     * Retorna las cuentas bancarias de una entidad.
     * @param idEntidad código de la entidad
     * @return lista de cuentas bancarias
     */
    List<CuentaBancariaParticipe> selectByParent(Long idEntidad) throws Throwable;
}
