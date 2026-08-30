package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de una transferencia de una carga Petro, para las pantallas del paso 1 del cobro
 * (ver {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md} §2.1 — CONTRATO
 * CONGELADO, no cambiar la forma sin acordarlo con el frontend).
 */
public class TransferenciaCargaPetroDTO {

    private Long idTransferencia;
    private Long idCarga;
    private Long idCuentaBancaria;
    /** Número de la cuenta bancaria, para mostrar sin resolver el id en el cliente. */
    private String cuentaBancaria;
    private Long idBanco;
    private String nombreBanco;
    private Long idBancoExterno;
    private String nombreBancoExterno;
    private String cuentaOrigen;
    private String numero;
    private Double valor;
    private LocalDate fecha;
    private String observacion;
    /** 1 = vigente, 0 = anulada. */
    private Long estado;
    private String usuarioRegistro;
    private LocalDateTime fechaRegistro;

    public Long getIdTransferencia() {
        return idTransferencia;
    }

    public void setIdTransferencia(Long idTransferencia) {
        this.idTransferencia = idTransferencia;
    }

    public Long getIdCarga() {
        return idCarga;
    }

    public void setIdCarga(Long idCarga) {
        this.idCarga = idCarga;
    }

    public Long getIdCuentaBancaria() {
        return idCuentaBancaria;
    }

    public void setIdCuentaBancaria(Long idCuentaBancaria) {
        this.idCuentaBancaria = idCuentaBancaria;
    }

    public String getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(String cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public Long getIdBanco() {
        return idBanco;
    }

    public void setIdBanco(Long idBanco) {
        this.idBanco = idBanco;
    }

    public String getNombreBanco() {
        return nombreBanco;
    }

    public void setNombreBanco(String nombreBanco) {
        this.nombreBanco = nombreBanco;
    }

    public Long getIdBancoExterno() {
        return idBancoExterno;
    }

    public void setIdBancoExterno(Long idBancoExterno) {
        this.idBancoExterno = idBancoExterno;
    }

    public String getNombreBancoExterno() {
        return nombreBancoExterno;
    }

    public void setNombreBancoExterno(String nombreBancoExterno) {
        this.nombreBancoExterno = nombreBancoExterno;
    }

    public String getCuentaOrigen() {
        return cuentaOrigen;
    }

    public void setCuentaOrigen(String cuentaOrigen) {
        this.cuentaOrigen = cuentaOrigen;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
