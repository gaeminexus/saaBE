package com.saa.ejb.crd.service.dto;

import com.saa.model.crd.Adjunto;
import com.saa.model.crd.CuentaBancariaBeneficiario;

/**
 * Resultado de {@code CuentaBancariaBeneficiarioService.crearConCertificado}: el beneficiario y
 * su certificado, ya guardados los dos en la misma transacción.
 */
public class ResultadoCuentaBancariaBeneficiarioConCertificado {

    private CuentaBancariaBeneficiario beneficiario;
    private Adjunto certificado;

    public ResultadoCuentaBancariaBeneficiarioConCertificado() {
    }

    public ResultadoCuentaBancariaBeneficiarioConCertificado(CuentaBancariaBeneficiario beneficiario, Adjunto certificado) {
        this.beneficiario = beneficiario;
        this.certificado = certificado;
    }

    public CuentaBancariaBeneficiario getBeneficiario() {
        return beneficiario;
    }

    public void setBeneficiario(CuentaBancariaBeneficiario beneficiario) {
        this.beneficiario = beneficiario;
    }

    public Adjunto getCertificado() {
        return certificado;
    }

    public void setCertificado(Adjunto certificado) {
        this.certificado = certificado;
    }
}
