package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DetalleAportePrecancelacion;

import jakarta.ejb.Local;

/**
 * Interface DAO para la entidad DetalleAportePrecancelacion (CRD.DAPR): el desglose por tipo
 * de aporte CONSUMIDO para cubrir parte de una precancelación registrada en {@code CRD.CBCR}.
 */
@Local
public interface DetalleAportePrecancelacionDaoService extends EntityDao<DetalleAportePrecancelacion> {

    /**
     * Las líneas de aporte consumidas por una línea de cobro (CRD.DCBC).
     *
     * @param idDetalleCobro : Código de la línea del cobro (CRD.DCBC.DCBCCDGO)
     * @return               : Listado; vacío si la línea no consume aportes
     * @throws Throwable     : Excepcion
     */
    List<DetalleAportePrecancelacion> selectByDetalleCobro(Long idDetalleCobro) throws Throwable;
}
