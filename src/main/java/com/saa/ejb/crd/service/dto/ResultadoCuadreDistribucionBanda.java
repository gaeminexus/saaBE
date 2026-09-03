package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de {@code GET /rest/dsbn/cuadre} — API-AUDITORIA-BANDAS.md §1. El encabezado que
 * abre la pantalla, ANTES del detalle (decisión del usuario, PLAN-AUDITORIA-BANDAS.md §4).
 *
 * <p>{@code recibido}/{@code diferencia}/{@code cuadra} vienen {@code null} cuando este origen
 * todavía no tiene una fuente de "recibido" independiente conectada (2026-09-02: solo
 * {@code CARGA_PETRO} la tiene) — es una limitación de cobertura, no un error; la pantalla
 * debe mostrar el resto igual.</p>
 */
public class ResultadoCuadreDistribucionBanda {

    private String origen;
    private Long idOrigen;
    private String descripcionOrigen;
    private Double recibido;
    private double distribuido;
    private Double diferencia;
    private Boolean cuadra;
    private boolean contabilidadConectada;
    private List<AsientoResumenDsbn> asientos = new ArrayList<>();
    private List<BandaProductoDetalle> bandas = new ArrayList<>();

    public ResultadoCuadreDistribucionBanda() {
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

    public String getDescripcionOrigen() {
        return descripcionOrigen;
    }

    public void setDescripcionOrigen(String descripcionOrigen) {
        this.descripcionOrigen = descripcionOrigen;
    }

    public Double getRecibido() {
        return recibido;
    }

    public void setRecibido(Double recibido) {
        this.recibido = recibido;
    }

    public double getDistribuido() {
        return distribuido;
    }

    public void setDistribuido(double distribuido) {
        this.distribuido = distribuido;
    }

    public Double getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(Double diferencia) {
        this.diferencia = diferencia;
    }

    public Boolean getCuadra() {
        return cuadra;
    }

    public void setCuadra(Boolean cuadra) {
        this.cuadra = cuadra;
    }

    public boolean isContabilidadConectada() {
        return contabilidadConectada;
    }

    public void setContabilidadConectada(boolean contabilidadConectada) {
        this.contabilidadConectada = contabilidadConectada;
    }

    public List<AsientoResumenDsbn> getAsientos() {
        return asientos;
    }

    public void setAsientos(List<AsientoResumenDsbn> asientos) {
        this.asientos = asientos;
    }

    public List<BandaProductoDetalle> getBandas() {
        return bandas;
    }

    public void setBandas(List<BandaProductoDetalle> bandas) {
        this.bandas = bandas;
    }
}
