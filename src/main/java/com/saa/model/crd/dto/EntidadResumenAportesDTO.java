package com.saa.model.crd.dto;

import java.io.Serializable;

/**
 * DTO para resumen de aportes por estado de entidad
 */
public class EntidadResumenAportesDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long estadoId;
    private Double totalAportes;
    
    public EntidadResumenAportesDTO() {
    }
    
    public EntidadResumenAportesDTO(Long estadoId, Double totalAportes) {
        this.estadoId = estadoId;
        this.totalAportes = totalAportes;
    }

    public Long getEstadoId() {
        return estadoId;
    }

    public void setEstadoId(Long estadoId) {
        this.estadoId = estadoId;
    }

    public Double getTotalAportes() {
        return totalAportes;
    }

    public void setTotalAportes(Double totalAportes) {
        this.totalAportes = totalAportes;
    }
}
