package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.FirmanteContrato;

import jakarta.ejb.Local;

/** Firmantes de un trámite de firma del contrato (CRD.FRFR). Solo lectura: los crea y actualiza {@link FirmaContratoService}. */
@Local
public interface FirmanteContratoService extends EntityService<FirmanteContrato> {

    /** Firmantes de un trámite, por orden de firma. */
    List<FirmanteContrato> selectByFirma(Long idFirma) throws Throwable;
}
