package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Cambio normativo de bandas: cierra la vigencia de la configuración actual y abre una
 * nueva a partir de {@code fechaDesdeNueva}. La vieja queda intacta para reprocesos y
 * auditoría de los períodos ya contabilizados.
 *
 * La configuración vieja se cierra en {@code fechaDesdeNueva - 1 día}, de modo que las
 * dos vigencias son contiguas y no se solapan ni dejan hueco.
 *
 * Este es el ÚNICO camino para cambiar una configuración cuya vigencia ya empezó.
 */
public class SolicitudCierreVigencia {

    /** Código de la configuración vigente que se cierra. Obligatorio. */
    private Long idConfiguracionVigente;

    /**
     * Fecha desde la que rige la configuración nueva. Obligatoria y posterior al
     * {@code fechaDesde} de la configuración que se cierra.
     */
    private LocalDate fechaDesdeNueva;

    /** Usuario que ejecuta la operación, para la auditoría. */
    private String usuario;

    /** IP desde la que se ejecuta, para la auditoría. */
    private String ip;

    /** Bandas de la configuración NUEVA. Obligatoria y no vacía. */
    private List<SolicitudBanda> bandas = new ArrayList<SolicitudBanda>();

    public SolicitudCierreVigencia() {
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

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<SolicitudBanda> getBandas() {
        return bandas;
    }

    public void setBandas(List<SolicitudBanda> bandas) {
        this.bandas = bandas;
    }
}
