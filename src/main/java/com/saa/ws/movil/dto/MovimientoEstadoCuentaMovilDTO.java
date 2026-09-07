package com.saa.ws.movil.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Espejo movil de {@code MovimientoEstadoCuentaDTO}, con fecha como texto (§4.3 del contrato). */
public class MovimientoEstadoCuentaMovilDTO {

    private Long idAporte;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaTransaccion;

    private Double valor;
    private Long tipoMovimiento;
    private String tipoMovimientoTexto;
    private String glosa;

    public Long getIdAporte() {
        return idAporte;
    }

    public void setIdAporte(Long idAporte) {
        this.idAporte = idAporte;
    }

    public LocalDateTime getFechaTransaccion() {
        return fechaTransaccion;
    }

    public void setFechaTransaccion(LocalDateTime fechaTransaccion) {
        this.fechaTransaccion = fechaTransaccion;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(Long tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public String getTipoMovimientoTexto() {
        return tipoMovimientoTexto;
    }

    public void setTipoMovimientoTexto(String tipoMovimientoTexto) {
        this.tipoMovimientoTexto = tipoMovimientoTexto;
    }

    public String getGlosa() {
        return glosa;
    }

    public void setGlosa(String glosa) {
        this.glosa = glosa;
    }
}
