package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

import com.saa.model.crd.FirmaContrato;
import com.saa.model.crd.FirmanteContrato;

/** Un trámite de firma del contrato con sus firmantes (lo que consume la tarjeta «Firma electrónica» del contrato). */
public class TramiteFirma {

    private FirmaContrato firma;
    private List<FirmanteContrato> firmantes = new ArrayList<>();

    public TramiteFirma() {
    }

    public TramiteFirma(FirmaContrato firma, List<FirmanteContrato> firmantes) {
        this.firma = firma;
        this.firmantes = firmantes;
    }

    public FirmaContrato getFirma() { return firma; }
    public void setFirma(FirmaContrato firma) { this.firma = firma; }
    public List<FirmanteContrato> getFirmantes() { return firmantes; }
    public void setFirmantes(List<FirmanteContrato> firmantes) { this.firmantes = firmantes; }
}
