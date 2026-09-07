package com.saa.ws.movil.dto;

import java.util.ArrayList;
import java.util.List;

/** Espejo movil de {@code PeriodoEstadoCuentaDTO} — ver esa clase para el significado de cada campo. */
public class PeriodoEstadoCuentaMovilDTO {

    private String periodo;
    private Long idTipoAporte;
    private String nombreTipoAporte;
    private Double esperado;
    private Double aportado;
    private Double faltante;
    private String estado;
    private List<MovimientoEstadoCuentaMovilDTO> movimientos = new ArrayList<>();

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public String getNombreTipoAporte() {
        return nombreTipoAporte;
    }

    public void setNombreTipoAporte(String nombreTipoAporte) {
        this.nombreTipoAporte = nombreTipoAporte;
    }

    public Double getEsperado() {
        return esperado;
    }

    public void setEsperado(Double esperado) {
        this.esperado = esperado;
    }

    public Double getAportado() {
        return aportado;
    }

    public void setAportado(Double aportado) {
        this.aportado = aportado;
    }

    public Double getFaltante() {
        return faltante;
    }

    public void setFaltante(Double faltante) {
        this.faltante = faltante;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<MovimientoEstadoCuentaMovilDTO> getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(List<MovimientoEstadoCuentaMovilDTO> movimientos) {
        this.movimientos = movimientos;
    }
}
