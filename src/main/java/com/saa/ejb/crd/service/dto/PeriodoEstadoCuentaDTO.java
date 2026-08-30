package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Un periodo (mes de devengo efectivo + tipo de aporte) del estado de cuenta de aportes por
 * devengo (§4.2 del plan de devengo de aportes).
 *
 * {@code periodo} es {@code "yyyy-MM"}, o {@code null} para el grupo "SIN PERIODO" (los
 * movimientos cuyo periodo efectivo es NULL: históricos sin backfillear y retiros de saldo —
 * ver {@code PeriodoEfectivoAporteSql}). Ese grupo no se esconde.
 *
 * {@code estado}: {@code "COMPLETO"} (aportado cubre esperado), {@code "PARCIAL"} (aportado
 * &gt; 0 pero no cubre esperado), {@code "SIN APORTE"} (nada aportado, había esperado),
 * {@code "ANTICIPADO"} (periodo posterior al mes en curso con aportado &gt; 0), o
 * {@code "SIN PERIODO"} (el grupo sin devengo).
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
public class PeriodoEstadoCuentaDTO {

    private String periodo;
    private Long idTipoAporte;
    private String nombreTipoAporte;
    private Double esperado;
    private Double aportado;
    private Double faltante;
    private String estado;
    private List<MovimientoEstadoCuentaDTO> movimientos = new ArrayList<>();

    public PeriodoEstadoCuentaDTO() {
    }

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

    public List<MovimientoEstadoCuentaDTO> getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(List<MovimientoEstadoCuentaDTO> movimientos) {
        this.movimientos = movimientos;
    }
}
