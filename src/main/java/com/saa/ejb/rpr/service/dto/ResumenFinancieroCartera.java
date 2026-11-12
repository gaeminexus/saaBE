package com.saa.ejb.rpr.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * C27-E — Resumen Financiero de una ejecución de Informes Mensuales (RPR.EJCC), armado en el
 * backend: cada línea con su cuenta contable y la diferencia contra el balance al corte, y el
 * aviso de ejecución desactualizada (generada antes del cierre contable o de cartera del mes).
 */
public class ResumenFinancieroCartera {

    private Long codigoEjecucion;
    private Long mes;
    private Long anio;
    private LocalDate corte;
    private LocalDateTime fechaGeneracion;
    /** Último cierre contable del período (CNT.PRDO.PRDOFCCN). */
    private LocalDateTime fechaCierreContable;
    /** Última corrida EJECUTADA del cierre de cartera del mes (CRD.CRCT). */
    private LocalDateTime fechaCierreCartera;
    /** true si el período contable del mes está cerrado. */
    private boolean periodoCerrado;
    /** true si la ejecución es anterior al último cierre contable o de cartera, o el período está abierto. */
    private boolean desactualizada;
    /** Texto del aviso (vacío si la ejecución está al día). */
    private String aviso;
    private List<LineaResumenFinanciero> lineas = new ArrayList<>();

    public Long getCodigoEjecucion() { return codigoEjecucion; }
    public void setCodigoEjecucion(Long codigoEjecucion) { this.codigoEjecucion = codigoEjecucion; }
    public Long getMes() { return mes; }
    public void setMes(Long mes) { this.mes = mes; }
    public Long getAnio() { return anio; }
    public void setAnio(Long anio) { this.anio = anio; }
    public LocalDate getCorte() { return corte; }
    public void setCorte(LocalDate corte) { this.corte = corte; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDateTime fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
    public LocalDateTime getFechaCierreContable() { return fechaCierreContable; }
    public void setFechaCierreContable(LocalDateTime fechaCierreContable) { this.fechaCierreContable = fechaCierreContable; }
    public LocalDateTime getFechaCierreCartera() { return fechaCierreCartera; }
    public void setFechaCierreCartera(LocalDateTime fechaCierreCartera) { this.fechaCierreCartera = fechaCierreCartera; }
    public boolean isPeriodoCerrado() { return periodoCerrado; }
    public void setPeriodoCerrado(boolean periodoCerrado) { this.periodoCerrado = periodoCerrado; }
    public boolean isDesactualizada() { return desactualizada; }
    public void setDesactualizada(boolean desactualizada) { this.desactualizada = desactualizada; }
    public String getAviso() { return aviso; }
    public void setAviso(String aviso) { this.aviso = aviso; }
    public List<LineaResumenFinanciero> getLineas() { return lineas; }
    public void setLineas(List<LineaResumenFinanciero> lineas) { this.lineas = lineas; }
}
