package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Vista segura de {@link com.saa.model.crd.UsuarioApp} para las respuestas REST de
 * {@code crear}/{@code resetearClave}/{@code reactivar} — a propósito NUNCA incluye
 * {@code claveHash}: devolver el hash de la clave en una respuesta HTTP es la misma
 * exposición innecesaria que justificó no publicar {@code usap/getAll}/{@code getId}.
 *
 * <p><b>Fechas:</b> {@code bloqueadoHasta}, {@code fechaCreacion} y
 * {@code fechaUltimoAcceso} son {@link java.time.LocalDateTime}. Esta app serializa con
 * Jackson (no JSON-B — ver CLAUDE.md), así que viajan como arreglo
 * {@code [año,mes,día,hora,minuto,segundo]} (ej. {@code [2026,9,3,14,30,0]}), NO como
 * texto ISO. Es el comportamiento de todo el sistema, no una particularidad de este DTO —
 * el consumidor de intranet tiene que parsearlas como arreglo.
 */
public class UsuarioAppDTO {

    private Long codigo;
    private Long idEntidad;
    private String identificacion;
    private Long estado;
    private Long intentosFallidos;
    private LocalDateTime bloqueadoHasta;
    private Boolean debeCambiarClave;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimoAcceso;
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Long getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(Long intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public Boolean getDebeCambiarClave() {
        return debeCambiarClave;
    }

    public void setDebeCambiarClave(Boolean debeCambiarClave) {
        this.debeCambiarClave = debeCambiarClave;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaUltimoAcceso() {
        return fechaUltimoAcceso;
    }

    public void setFechaUltimoAcceso(LocalDateTime fechaUltimoAcceso) {
        this.fechaUltimoAcceso = fechaUltimoAcceso;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
