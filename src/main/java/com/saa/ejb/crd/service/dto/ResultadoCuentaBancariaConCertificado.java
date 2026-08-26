package com.saa.ejb.crd.service.dto;

import com.saa.model.crd.Adjunto;
import com.saa.model.crd.CuentaBancariaParticipe;

/**
 * Resultado de {@code CuentaBancariaParticipeService.crearConCertificado}: la cuenta y su
 * certificado, ya guardados los dos en la misma transacción.
 */
public class ResultadoCuentaBancariaConCertificado {

    private CuentaBancariaParticipe cuenta;
    private Adjunto certificado;

    public ResultadoCuentaBancariaConCertificado() {
    }

    public ResultadoCuentaBancariaConCertificado(CuentaBancariaParticipe cuenta, Adjunto certificado) {
        this.cuenta = cuenta;
        this.certificado = certificado;
    }

    public CuentaBancariaParticipe getCuenta() {
        return cuenta;
    }

    public void setCuenta(CuentaBancariaParticipe cuenta) {
        this.cuenta = cuenta;
    }

    public Adjunto getCertificado() {
        return certificado;
    }

    public void setCertificado(Adjunto certificado) {
        this.certificado = certificado;
    }
}
