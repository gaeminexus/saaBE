package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Body de {@code POST /rest/vgcn}, forma congelada en
 * docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md §4.1. No trae {@code remuneracion}:
 * en modo CALCULADO la resuelve el propio servicio desde {@code CRD.PRTC.PRTCRMUN} del
 * partícipe del contrato, al momento de crear la vigencia (D8: el porcentaje sólo recalcula
 * el monto al crear, nunca al vuelo).
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
public class SolicitudVigenciaContrato {

    private Long idContrato;
    private Long idTipoAporte;
    private LocalDate fechaInicio;
    private Long modo;
    private Double monto;
    private Double porcentaje;
    private String observacion;
    private String usuario;

    public Long getIdContrato() {
        return idContrato;
    }

    public void setIdContrato(Long idContrato) {
        this.idContrato = idContrato;
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Long getModo() {
        return modo;
    }

    public void setModo(Long modo) {
        this.modo = modo;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}
