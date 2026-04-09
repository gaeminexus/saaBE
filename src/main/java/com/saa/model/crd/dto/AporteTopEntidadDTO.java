package com.saa.model.crd.dto;

import java.io.Serializable;

/**
 * DTO para top entidades por tipo de aporte
 */
public class AporteTopEntidadDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long tipoAporteId;
    private Long entidadId;
    private String entidadNombre;
    private Long movimientos;
    private Double montoMas;
    private Double montoMenos;
    private Double saldoNeto;
    
    public AporteTopEntidadDTO() {
    }
    
    public AporteTopEntidadDTO(Long tipoAporteId, Long entidadId, String entidadNombre, 
                               Long movimientos, Double montoMas, Double montoMenos, 
                               Double saldoNeto) {
        this.tipoAporteId = tipoAporteId;
        this.entidadId = entidadId;
        this.entidadNombre = entidadNombre;
        this.movimientos = movimientos;
        this.montoMas = montoMas;
        this.montoMenos = montoMenos;
        this.saldoNeto = saldoNeto;
    }

    // Getters y Setters
    public Long getTipoAporteId() {
        return tipoAporteId;
    }

    public void setTipoAporteId(Long tipoAporteId) {
        this.tipoAporteId = tipoAporteId;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }

    public String getEntidadNombre() {
        return entidadNombre;
    }

    public void setEntidadNombre(String entidadNombre) {
        this.entidadNombre = entidadNombre;
    }

    public Long getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(Long movimientos) {
        this.movimientos = movimientos;
    }

    public Double getMontoMas() {
        return montoMas;
    }

    public void setMontoMas(Double montoMas) {
        this.montoMas = montoMas;
    }

    public Double getMontoMenos() {
        return montoMenos;
    }

    public void setMontoMenos(Double montoMenos) {
        this.montoMenos = montoMenos;
    }

    public Double getSaldoNeto() {
        return saldoNeto;
    }

    public void setSaldoNeto(Double saldoNeto) {
        this.saldoNeto = saldoNeto;
    }
}
