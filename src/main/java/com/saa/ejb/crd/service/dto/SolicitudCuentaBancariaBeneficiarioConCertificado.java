package com.saa.ejb.crd.service.dto;

import java.io.InputStream;
import java.math.BigDecimal;

/**
 * Cuerpo de {@code CuentaBancariaBeneficiarioService.crearConCertificado}. Lo arma
 * {@code CuentaBancariaBeneficiarioRest} a partir de un multipart — no viaja como JSON.
 *
 * Calcado de {@code SolicitudCuentaBancariaConCertificado} (CNBP), con los dos campos propios
 * del beneficiario que la cuenta del propio partícipe no necesita: {@code nombre} y
 * {@code numeroIdentificacion}.
 */
public class SolicitudCuentaBancariaBeneficiarioConCertificado {

    private Long idEntidad;
    private String nombre;
    private String numeroIdentificacion;
    private Long idBancoExterno;
    private Long tipoCuenta;
    private String numeroCuenta;
    private BigDecimal porcentaje;

    private InputStream archivo;

    /** Nombre original del PDF, ya decodificado (URLDecoder, UTF-8) por el REST. */
    private String nombreArchivo;

    private String usuarioRegistro;

    public SolicitudCuentaBancariaBeneficiarioConCertificado() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public void setNumeroIdentificacion(String numeroIdentificacion) {
        this.numeroIdentificacion = numeroIdentificacion;
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

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
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
