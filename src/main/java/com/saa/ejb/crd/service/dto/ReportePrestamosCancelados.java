package com.saa.ejb.crd.service.dto;

import java.io.Serializable;
import java.util.List;

/** Respuesta de POST /rest/prcn/consultar (contrato §2.3). Fechas como String. */
public class ReportePrestamosCancelados implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fechaDesde;
    private String fechaHasta;
    private String generado;
    private Long cantidad;
    private Long canceladosSinFecha;
    private List<ResumenFormaCancelacion> resumenPorForma;
    private ResumenFormaCancelacion totales;
    private List<PrestamoCanceladoFila> filas;

    public String getFechaDesde() { return fechaDesde; }
    public void setFechaDesde(String fechaDesde) { this.fechaDesde = fechaDesde; }
    public String getFechaHasta() { return fechaHasta; }
    public void setFechaHasta(String fechaHasta) { this.fechaHasta = fechaHasta; }
    public String getGenerado() { return generado; }
    public void setGenerado(String generado) { this.generado = generado; }
    public Long getCantidad() { return cantidad; }
    public void setCantidad(Long cantidad) { this.cantidad = cantidad; }
    public Long getCanceladosSinFecha() { return canceladosSinFecha; }
    public void setCanceladosSinFecha(Long canceladosSinFecha) { this.canceladosSinFecha = canceladosSinFecha; }
    public List<ResumenFormaCancelacion> getResumenPorForma() { return resumenPorForma; }
    public void setResumenPorForma(List<ResumenFormaCancelacion> resumenPorForma) { this.resumenPorForma = resumenPorForma; }
    public ResumenFormaCancelacion getTotales() { return totales; }
    public void setTotales(ResumenFormaCancelacion totales) { this.totales = totales; }
    public List<PrestamoCanceladoFila> getFilas() { return filas; }
    public void setFilas(List<PrestamoCanceladoFila> filas) { this.filas = filas; }
}
