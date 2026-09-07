package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CxcParticipe;

import jakarta.ejb.Local;

@Local
public interface CxcParticipeDaoService extends EntityDao<CxcParticipe> {

    /**
     * Cuentas por cobrar (CRD.CXCP) de un partícipe. Agregado para {@code /movil} (§5.3 del
     * contrato de intranet-movil): consumido por {@code GET /movil/cuenta-individual/{idEntidad}}
     * para llegar al kardex CXC del partícipe.
     *
     * @param codigoEntidad : Código de la entidad (partícipe)
     * @return              : Cuentas por cobrar de la entidad; lista VACÍA si no tiene ninguna
     * @throws Throwable    : Excepcion
     */
    List<CxcParticipe> selectByEntidad(Long codigoEntidad) throws Throwable;
}
