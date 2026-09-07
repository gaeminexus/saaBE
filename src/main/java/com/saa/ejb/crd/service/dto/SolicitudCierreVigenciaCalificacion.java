package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Solicitud de {@code ConfiguracionCalificacionRiesgoService#cerrarVigencia} — P22. Mismo patrón
 * que {@code SolicitudCierreVigencia} (bandas): producto, empresa y nombre se heredan de la
 * configuración vigente, no se reenvían.
 */
public class SolicitudCierreVigenciaCalificacion {

    private Long idConfiguracionVigente;

    private LocalDate fechaDesdeNueva;

    private String usuario;

    private List<SolicitudEscala> escalas;

    public SolicitudCierreVigenciaCalificacion() {
    }

    public Long getIdConfiguracionVigente() {
        return idConfiguracionVigente;
    }

    public void setIdConfiguracionVigente(Long idConfiguracionVigente) {
        this.idConfiguracionVigente = idConfiguracionVigente;
    }

    public LocalDate getFechaDesdeNueva() {
        return fechaDesdeNueva;
    }

    public void setFechaDesdeNueva(LocalDate fechaDesdeNueva) {
        this.fechaDesdeNueva = fechaDesdeNueva;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public List<SolicitudEscala> getEscalas() {
        return escalas;
    }

    public void setEscalas(List<SolicitudEscala> escalas) {
        this.escalas = escalas;
    }
}
