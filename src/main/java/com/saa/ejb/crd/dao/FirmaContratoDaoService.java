package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.FirmaContrato;

import jakarta.ejb.Local;

@Local
public interface FirmaContratoDaoService extends EntityDao<FirmaContrato> {

    /**
     * Historial de trámites de firma de un contrato, el más reciente primero.
     *
     * @param idContrato : Código del contrato (CRD.CNTR)
     * @throws Throwable : Excepcion
     */
    List<FirmaContrato> selectByContrato(Long idContrato) throws Throwable;

    /**
     * Trámites vivos (PENDIENTE_ENVIO o ENVIADO) de un contrato. Debe haber uno como máximo (índice único UX_FRCN_VIVO);
     * devuelve lista para que el servicio valide sin depender de eso.
     *
     * @param idContrato : Código del contrato (CRD.CNTR)
     * @throws Throwable : Excepcion
     */
    List<FirmaContrato> selectVivosByContrato(Long idContrato) throws Throwable;
}
