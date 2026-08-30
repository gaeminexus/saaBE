package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Estado del flag del camino nuevo de generación de aportes por faltante (rubro 242,
 * Fase 4 del plan de devengo de aportes), con la huella de quién y cuándo lo cambió por
 * última vez.
 *
 * La huella vive codificada en {@code PDTRVLRV} (ver
 * {@link com.saa.ejb.crd.serviceImpl.ConfiguracionGeneracionAportesServiceImpl}), mismo
 * patrón que {@link EstadoContabilidadCrd}. Los tres campos de huella vienen en
 * {@code null} cuando todavía no se ha hecho ningún cambio, o cuando el valor guardado no
 * se pudo interpretar — nunca se lanza una excepción por eso.
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
public class EstadoGeneracionPorFaltante {

    private boolean activa;
    private String usuarioUltimoCambio;
    private LocalDateTime fechaUltimoCambio;
    private String motivoUltimoCambio;

    public EstadoGeneracionPorFaltante() {
    }

    public EstadoGeneracionPorFaltante(boolean activa) {
        this.activa = activa;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public String getUsuarioUltimoCambio() {
        return usuarioUltimoCambio;
    }

    public void setUsuarioUltimoCambio(String usuarioUltimoCambio) {
        this.usuarioUltimoCambio = usuarioUltimoCambio;
    }

    public LocalDateTime getFechaUltimoCambio() {
        return fechaUltimoCambio;
    }

    public void setFechaUltimoCambio(LocalDateTime fechaUltimoCambio) {
        this.fechaUltimoCambio = fechaUltimoCambio;
    }

    public String getMotivoUltimoCambio() {
        return motivoUltimoCambio;
    }

    public void setMotivoUltimoCambio(String motivoUltimoCambio) {
        this.motivoUltimoCambio = motivoUltimoCambio;
    }
}
