package com.saa.model.crd.dto;

import java.io.Serializable;

/**
 * DTO para KPIs globales de aportes del dashboard
 */
public class AporteKpiDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long movimientos;
    private Long tiposAporte;
    private Double montoMas;
    private Double montoMenos;
    private Double saldoNeto;
    
    public AporteKpiDTO() {
    }
    
    public AporteKpiDTO(Long movimientos, Long tiposAporte, Double montoMas, Double montoMenos, Double saldoNeto) {
        this.movimientos = movimientos;
        this.tiposAporte = tiposAporte;
        this.montoMas = montoMas;
        this.montoMenos = montoMenos;
        this.saldoNeto = saldoNeto;
    }

    public Long getMovimientos() {
        return movimientos;
    }

    public void setMovimientos(Long movimientos) {
        this.movimientos = movimientos;
    }

    public Long getTiposAporte() {
        return tiposAporte;
    }

    public void setTiposAporte(Long tiposAporte) {
        this.tiposAporte = tiposAporte;
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
