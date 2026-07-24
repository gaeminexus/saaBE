package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CuentaBancariaParticipe;

import jakarta.ejb.Local;

@Local
public interface CuentaBancariaParticipeDaoService extends EntityDao<CuentaBancariaParticipe> {

    /**
     * Retorna las cuentas bancarias de una entidad.
     * @param idEntidad código de la entidad
     * @return lista de cuentas bancarias
     */
    List<CuentaBancariaParticipe> selectByParent(Long idEntidad);
}
