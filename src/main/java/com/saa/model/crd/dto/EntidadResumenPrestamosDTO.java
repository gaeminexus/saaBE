package com.saa.model.crd.dto;

import java.io.Serializable;

/**
 * DTO para resumen de préstamos por estado de entidad
 */
public class EntidadResumenPrestamosDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long estadoId;
    private Double totalPrestamos;
    
    public EntidadResumenPrestamosDTO() {
    }
    
    public EntidadResumenPrestamosDTO(Long estadoId, Double totalPrestamos) {
        this.estadoId = estadoId;
        this.totalPrestamos = totalPrestamos;
    }

    public Long getEstadoId() {
        return estadoId;
    }

    public void setEstadoId(Long estadoId) {
        this.estadoId = estadoId;
    }

    public Double getTotalPrestamos() {
        return totalPrestamos;
    }

    public void setTotalPrestamos(Double totalPrestamos) {
        this.totalPrestamos = totalPrestamos;
    }
}
