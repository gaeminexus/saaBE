package com.saa.ejb.crd.service.dto;

import java.io.InputStream;

/**
 * Cuerpo de {@code CuentaBancariaParticipeService.crearConCertificado}. Lo arma
 * {@code CuentaBancariaParticipeRest} a partir de un multipart — no viaja como JSON.
 *
 * Regla que existe para cumplir: no se puede registrar una cuenta bancaria de un partícipe sin
 * adjuntar el PDF del certificado bancario. Ver
 * {@code CuentaBancariaParticipeServiceImpl.crearConCertificado} para la transacción completa.
 */
public class SolicitudCuentaBancariaConCertificado {

    private Long idEntidad;
    private Long idBancoExterno;
    private Long tipoCuenta;
    private String numeroCuenta;

    private InputStream archivo;

    /** Nombre original del PDF, ya decodificado (URLDecoder, UTF-8) por el REST. */
    private String nombreArchivo;

    private String usuarioRegistro;

    public SolicitudCuentaBancariaConCertificado() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public Long getIdBancoExterno() {
        return idBancoExterno;
    }

    public void setIdBancoExterno(Long idBancoExterno) {
        this.idBancoExterno = idBancoExterno;
    }

    public Long getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(Long tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public InputStream getArchivo() {
        return archivo;
    }

    public void setArchivo(InputStream archivo) {
        this.archivo = archivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
