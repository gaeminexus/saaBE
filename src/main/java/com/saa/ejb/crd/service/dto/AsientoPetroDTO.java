package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una fila de {@code GET /rest/asgn/estadoContable/{idCarga}} — CONTRATO CONGELADO, ver
 * {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md} §2.4.
 */
public class AsientoPetroDTO {

    /** 1 TRANSITORIO, 2 REPARTO, 3 APLICACION — ver {@link com.saa.rubros.SubProcesoCobroPetro}. */
    private Long tipo;
    /** El backend resuelve el texto, no el cliente. */
    private String tipoTexto;
    private Long idAsiento;
    private String numeroAsiento;
    private LocalDate fecha;
    private Double valor;
    private Long lineas;
    /** 1 vigente, 0 reversado. */
    private Long estado;
    private String usuarioRegistro;
    private LocalDateTime fechaRegistro;

    public Long getTipo() {
        return tipo;
    }

    public void setTipo(Long tipo) {
        this.tipo = tipo;
    }

    public String getTipoTexto() {
        return tipoTexto;
    }

    public void setTipoTexto(String tipoTexto) {
        this.tipoTexto = tipoTexto;
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

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getLineas() {
        return lineas;
    }

    public void setLineas(Long lineas) {
        this.lineas = lineas;
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
