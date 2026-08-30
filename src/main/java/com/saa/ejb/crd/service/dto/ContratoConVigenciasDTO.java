package com.saa.ejb.crd.service.dto;

import java.util.List;

/**
 * Forma de respuesta de {@code GET /rest/cntr/porEntidad/{idEntidad}}, congelada en
 * docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md §4.1.
 *
 * Actualización del 2026-08-27: la entidad SIN contrato activo ya no es un 404 — es un
 * estado válido (todavía no se le ha creado un contrato). En ese caso {@code idContrato},
 * {@code estado}, {@code estadoTexto}, los montos y los porcentajes viajan {@code null} y
 * {@code vigencias} viaja vacía; {@code idEntidad}/{@code identificacion}/{@code razonSocial}
 * siempre vienen desde {@code CRD.ENTD}. El 404 queda solo para cuando la ENTIDAD no existe.
 * {@code estadoTexto} es nuevo: mismo criterio que {@code modoTexto} de {@link VigenciaDTO} —
 * el backend resuelve el catálogo, el cliente no lo inventa.
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
public class ContratoConVigenciasDTO {

    private Long idContrato;
    private Long idEntidad;
    private String identificacion;
    private String razonSocial;
    private Long estado;
    private String estadoTexto;
    private Double montoJubilacion;
    private Double montoCesantia;
    private Double porcentajeJubilacion;
    private Double porcentajeCesantia;
    private Double remuneracionUnificada;
    private List<VigenciaDTO> vigencias;

    public Long getIdContrato() {
        return idContrato;
    }

    public void setIdContrato(Long idContrato) {
        this.idContrato = idContrato;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getEstadoTexto() {
        return estadoTexto;
    }

    public void setEstadoTexto(String estadoTexto) {
        this.estadoTexto = estadoTexto;
    }

    public Double getMontoJubilacion() {
        return montoJubilacion;
    }

    public void setMontoJubilacion(Double montoJubilacion) {
        this.montoJubilacion = montoJubilacion;
    }

    public Double getMontoCesantia() {
        return montoCesantia;
    }

    public void setMontoCesantia(Double montoCesantia) {
        this.montoCesantia = montoCesantia;
    }

    public Double getPorcentajeJubilacion() {
        return porcentajeJubilacion;
    }

    public void setPorcentajeJubilacion(Double porcentajeJubilacion) {
        this.porcentajeJubilacion = porcentajeJubilacion;
    }

    public Double getPorcentajeCesantia() {
        return porcentajeCesantia;
    }

    public void setPorcentajeCesantia(Double porcentajeCesantia) {
        this.porcentajeCesantia = porcentajeCesantia;
    }

    public Double getRemuneracionUnificada() {
        return remuneracionUnificada;
    }

    public void setRemuneracionUnificada(Double remuneracionUnificada) {
        this.remuneracionUnificada = remuneracionUnificada;
    }

    public List<VigenciaDTO> getVigencias() {
        return vigencias;
    }

    public void setVigencias(List<VigenciaDTO> vigencias) {
        this.vigencias = vigencias;
    }
}
