package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.FirmanteContrato;

import jakarta.ejb.Local;

@Local
public interface FirmanteContratoDaoService extends EntityDao<FirmanteContrato> {

    /**
     * Firmantes de un trámite, por orden de firma.
     *
     * @param idFirma    : Código del trámite (CRD.FRCN)
     * @throws Throwable : Excepcion
     */
    List<FirmanteContrato> selectByFirma(Long idFirma) throws Throwable;
}
