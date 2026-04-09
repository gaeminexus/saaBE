package com.saa.model.crd.dto;

import java.io.Serializable;

/**
 * DTO para resumen consolidado de entidad con préstamos y aportes
 */
public class EntidadResumenConsolidadoDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long estadoId;
    private Long totalEntidades;
    private Double totalPrestamos;
    private Double totalAportes;
    
    public EntidadResumenConsolidadoDTO() {
    }
    
    public EntidadResumenConsolidadoDTO(Long estadoId, Long totalEntidades, 
                                        Double totalPrestamos, Double totalAportes) {
        this.estadoId = estadoId;
        this.totalEntidades = totalEntidades;
        this.totalPrestamos = totalPrestamos;
        this.totalAportes = totalAportes;
    }

    public Long getEstadoId() {
        return estadoId;
    }

    public void setEstadoId(Long estadoId) {
        this.estadoId = estadoId;
    }

    public Long getTotalEntidades() {
        return totalEntidades;
    }

    public void setTotalEntidades(Long totalEntidades) {
        this.totalEntidades = totalEntidades;
    }

    public Double getTotalPrestamos() {
        return totalPrestamos;
    }

    public void setTotalPrestamos(Double totalPrestamos) {
        this.totalPrestamos = totalPrestamos;
    }

    public Double getTotalAportes() {
        return totalAportes;
    }

    public void setTotalAportes(Double totalAportes) {
        this.totalAportes = totalAportes;
    }
}
