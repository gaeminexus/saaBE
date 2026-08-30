package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de {@code GET /rest/aprt/estadoCuenta/{idEntidad}} (§4.2 del plan de devengo de
 * aportes). Contrato CONGELADO — el frontend ya está construido contra él.
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
public class EstadoCuentaAportesDTO {

    private Long idEntidad;
    private String identificacion;
    private String razonSocial;
    private List<PeriodoEstadoCuentaDTO> periodos = new ArrayList<>();
    private Double totalFaltante;

    public EstadoCuentaAportesDTO() {
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

    public List<PeriodoEstadoCuentaDTO> getPeriodos() {
        return periodos;
    }

    public void setPeriodos(List<PeriodoEstadoCuentaDTO> periodos) {
        this.periodos = periodos;
    }

    public Double getTotalFaltante() {
        return totalFaltante;
    }

    public void setTotalFaltante(Double totalFaltante) {
        this.totalFaltante = totalFaltante;
    }
}
