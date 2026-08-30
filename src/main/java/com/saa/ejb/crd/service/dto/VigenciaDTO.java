package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Forma de respuesta de una vigencia de contrato (CRD.VGCN), congelada en
 * docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md §4.1.
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
public class VigenciaDTO {

    private Long idVigencia;
    private Long idContrato;
    private Long idTipoAporte;
    private String nombreTipoAporte;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Double monto;
    private Double porcentaje;
    private Double remuneracion;
    private Long modo;
    private String modoTexto;
    private Long estado;
    private String observacion;

    public Long getIdVigencia() {
        return idVigencia;
    }

    public void setIdVigencia(Long idVigencia) {
        this.idVigencia = idVigencia;
    }

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

    public String getNombreTipoAporte() {
        return nombreTipoAporte;
    }

    public void setNombreTipoAporte(String nombreTipoAporte) {
        this.nombreTipoAporte = nombreTipoAporte;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
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

    public Double getRemuneracion() {
        return remuneracion;
    }

    public void setRemuneracion(Double remuneracion) {
        this.remuneracion = remuneracion;
    }

    public Long getModo() {
        return modo;
    }

    public void setModo(Long modo) {
        this.modo = modo;
    }

    public String getModoTexto() {
        return modoTexto;
    }

    public void setModoTexto(String modoTexto) {
        this.modoTexto = modoTexto;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
