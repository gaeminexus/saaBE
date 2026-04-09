package com.saa.model.crd.dto;

import java.io.Serializable;

/**
 * DTO para resumen de aportes agrupados por tipo (con dona/tarjetas)
 */
public class AporteResumenTipoDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long tipoAporteId;
    private String tipoAporteNombre;
    private Long movimientos;
    private Double montoMas;
    private Double montoMenos;
    private Double saldoNeto;
    private Double magnitudNeta;
    private Double porcentajeDona;
    
    public AporteResumenTipoDTO() {
    }
    
    public AporteResumenTipoDTO(Long tipoAporteId, String tipoAporteNombre, Long movimientos, 
                                Double montoMas, Double montoMenos, Double saldoNeto, 
                                Double magnitudNeta, Double porcentajeDona) {
        this.tipoAporteId = tipoAporteId;
        this.tipoAporteNombre = tipoAporteNombre;
        this.movimientos = movimientos;
        this.montoMas = montoMas;
        this.montoMenos = montoMenos;
        this.saldoNeto = saldoNeto;
        this.magnitudNeta = magnitudNeta;
        this.porcentajeDona = porcentajeDona;
    }

    // Getters y Setters
    public Long getTipoAporteId() {
        return tipoAporteId;
    }

    public void setTipoAporteId(Long tipoAporteId) {
        this.tipoAporteId = tipoAporteId;
    }

    public String getTipoAporteNombre() {
        return tipoAporteNombre;
    }

    public void setTipoAporteNombre(String tipoAporteNombre) {
        this.tipoAporteNombre = tipoAporteNombre;
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

    public Double getMagnitudNeta() {
        return magnitudNeta;
    }

    public void setMagnitudNeta(Double magnitudNeta) {
        this.magnitudNeta = magnitudNeta;
    }

    public Double getPorcentajeDona() {
        return porcentajeDona;
    }

    public void setPorcentajeDona(Double porcentajeDona) {
        this.porcentajeDona = porcentajeDona;
    }
}
