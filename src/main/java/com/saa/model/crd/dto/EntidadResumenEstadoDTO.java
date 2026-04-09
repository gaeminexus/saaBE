package com.saa.model.crd.dto;

import java.io.Serializable;

/**
 * DTO para resumen de entidades por estado
 */
public class EntidadResumenEstadoDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long estadoId;
    private Long totalEntidades;
    
    public EntidadResumenEstadoDTO() {
    }
    
    public EntidadResumenEstadoDTO(Long estadoId, Long totalEntidades) {
        this.estadoId = estadoId;
        this.totalEntidades = totalEntidades;
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
}
