package com.saa.ejb.crd.service.dto;

import java.io.Serializable;

/** Una entrada del resumen por forma de cancelación (contrato §2.3). Con forma y descripcion nulas es la forma de los totales generales. */
public class ResumenFormaCancelacion implements Serializable {

    private static final long serialVersionUID = 1L;

    private String forma;
    private String descripcion;
    private Long cantidad;
    private Double montoSolicitado;
    private Double totalPagado;
    private Double totalCapitalPagado;
    private Double totalInteresPagado;
    private Double totalMoraPagada;
    private Double totalCondonado;

    public String getForma() { return forma; }
    public void setForma(String forma) { this.forma = forma; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Long getCantidad() { return cantidad; }
    public void setCantidad(Long cantidad) { this.cantidad = cantidad; }
    public Double getMontoSolicitado() { return montoSolicitado; }
    public void setMontoSolicitado(Double montoSolicitado) { this.montoSolicitado = montoSolicitado; }
    public Double getTotalPagado() { return totalPagado; }
    public void setTotalPagado(Double totalPagado) { this.totalPagado = totalPagado; }
    public Double getTotalCapitalPagado() { return totalCapitalPagado; }
    public void setTotalCapitalPagado(Double totalCapitalPagado) { this.totalCapitalPagado = totalCapitalPagado; }
    public Double getTotalInteresPagado() { return totalInteresPagado; }
    public void setTotalInteresPagado(Double totalInteresPagado) { this.totalInteresPagado = totalInteresPagado; }
    public Double getTotalMoraPagada() { return totalMoraPagada; }
    public void setTotalMoraPagada(Double totalMoraPagada) { this.totalMoraPagada = totalMoraPagada; }
    public Double getTotalCondonado() { return totalCondonado; }
    public void setTotalCondonado(Double totalCondonado) { this.totalCondonado = totalCondonado; }
}
