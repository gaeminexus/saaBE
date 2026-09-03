package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/** Una fila de {@code GET /rest/dsbn/origenes} — API-AUDITORIA-BANDAS.md §3. */
public class OrigenDistribucionBandaResumen {

    private String origen;
    private Long idOrigen;
    private String descripcion;
    private LocalDate fecha;
    private double distribuido;

    /** {@code null} si este origen no tiene todavía una fuente de "recibido" conectada
     * (ver {@link ResultadoCuadreDistribucionBanda}). */
    private Boolean cuadra;

    public OrigenDistribucionBandaResumen() {
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Long getIdOrigen() {
        return idOrigen;
    }

    public void setIdOrigen(Long idOrigen) {
        this.idOrigen = idOrigen;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public double getDistribuido() {
        return distribuido;
    }

    public void setDistribuido(double distribuido) {
        this.distribuido = distribuido;
    }

    public Boolean getCuadra() {
        return cuadra;
    }

    public void setCuadra(Boolean cuadra) {
        this.cuadra = cuadra;
    }
}
