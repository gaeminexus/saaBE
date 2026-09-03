package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/** Un asiento vinculado a un origen — ver {@link ResultadoCuadreDistribucionBanda}. */
public class AsientoResumenDsbn {

    private Long idAsiento;
    private String tipo;
    private LocalDate fecha;
    private String estado;

    public AsientoResumenDsbn() {
    }

    public Long getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(Long idAsiento) {
        this.idAsiento = idAsiento;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
