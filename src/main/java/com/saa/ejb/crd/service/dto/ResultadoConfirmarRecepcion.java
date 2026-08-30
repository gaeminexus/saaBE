package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Respuesta de {@code POST /rest/asgn/confirmarRecepcion/{idCarga}} — CONTRATO CONGELADO,
 * ver {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md} §2.2.
 */
public class ResultadoConfirmarRecepcion {

    private Long idCarga;
    private Boolean confirmada;
    private Long idAsiento;
    private String numeroAsiento;
    private LocalDate fechaAsiento;
    private Double valorAsiento;
    /** false = se confirmó pero NO se generó asiento (contabilidad de CRD apagada). */
    private Boolean contabilidadActiva;
    private String mensaje;

    public Long getIdCarga() {
        return idCarga;
    }

    public void setIdCarga(Long idCarga) {
        this.idCarga = idCarga;
    }

    public Boolean getConfirmada() {
        return confirmada;
    }

    public void setConfirmada(Boolean confirmada) {
        this.confirmada = confirmada;
    }

    public Long getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(Long idAsiento) {
        this.idAsiento = idAsiento;
    }

    public String getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(String numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }

    public LocalDate getFechaAsiento() {
        return fechaAsiento;
    }

    public void setFechaAsiento(LocalDate fechaAsiento) {
        this.fechaAsiento = fechaAsiento;
    }

    public Double getValorAsiento() {
        return valorAsiento;
    }

    public void setValorAsiento(Double valorAsiento) {
        this.valorAsiento = valorAsiento;
    }

    public Boolean getContabilidadActiva() {
        return contabilidadActiva;
    }

    public void setContabilidadActiva(Boolean contabilidadActiva) {
        this.contabilidadActiva = contabilidadActiva;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
