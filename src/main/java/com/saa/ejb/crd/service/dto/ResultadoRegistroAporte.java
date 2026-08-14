package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Resultado de registrar un pago de aportes en ventanilla.
 */
public class ResultadoRegistroAporte {

    /** APRTCDGO de la fila creada */
    private Long idAporte;

    /** PGAPCDGO del PagoAporte creado */
    private Long idPagoAporte;

    private Long idEntidad;
    private Long idTipoAporte;
    private String nombreTipoAporte;

    /** Valor registrado */
    private Double valor;

    /** Saldo del tipo de aporte DESPUÉS del registro, para refrescar la pantalla */
    private Double saldoTipoAporte;

    private LocalDateTime fechaTransaccion;

    public ResultadoRegistroAporte() {
    }

    public Long getIdAporte() {
        return idAporte;
    }

    public void setIdAporte(Long idAporte) {
        this.idAporte = idAporte;
    }

    public Long getIdPagoAporte() {
        return idPagoAporte;
    }

    public void setIdPagoAporte(Long idPagoAporte) {
        this.idPagoAporte = idPagoAporte;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public String getNombreTipoAporte() {
        return nombreTipoAporte;
    }

    public void setNombreTipoAporte(String nombreTipoAporte) {
        this.nombreTipoAporte = nombreTipoAporte;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getSaldoTipoAporte() {
        return saldoTipoAporte;
    }

    public void setSaldoTipoAporte(Double saldoTipoAporte) {
        this.saldoTipoAporte = saldoTipoAporte;
    }

    public LocalDateTime getFechaTransaccion() {
        return fechaTransaccion;
    }

    public void setFechaTransaccion(LocalDateTime fechaTransaccion) {
        this.fechaTransaccion = fechaTransaccion;
    }
}
