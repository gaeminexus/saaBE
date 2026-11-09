package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de POST pgpc/procesarPendientes: paga solo a los jubilados con pensiones pendientes (retenidas o rechazadas
 * que volvieron a pendiente), sin recorrer el padrón ni tocar seguro, cruce ni la pensión del mes.
 */
public class ResultadoProcesoPendientes {
    private Integer anio;
    private Integer mes;
    /** Jubilados con pendientes que se evaluaron. */
    private int evaluados;
    /** Jubilados a los que se les pagó al menos un mes. */
    private int conPagos;
    /** Jubilados que quedaron con algo sin pagar (sin salida, saldo insuficiente). */
    private int conPendientesSinPagar;
    private int conError;
    private int mesesPagados;
    private double totalPagado;
    private double totalNoPagado;
    private List<ResultadoPagoRetenidos> detalle = new ArrayList<>();
    private List<String> errores = new ArrayList<>();

    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public Integer getMes() { return mes; }
    public void setMes(Integer mes) { this.mes = mes; }
    public int getEvaluados() { return evaluados; }
    public void setEvaluados(int evaluados) { this.evaluados = evaluados; }
    public int getConPagos() { return conPagos; }
    public void setConPagos(int conPagos) { this.conPagos = conPagos; }
    public int getConPendientesSinPagar() { return conPendientesSinPagar; }
    public void setConPendientesSinPagar(int conPendientesSinPagar) { this.conPendientesSinPagar = conPendientesSinPagar; }
    public int getConError() { return conError; }
    public void setConError(int conError) { this.conError = conError; }
    public int getMesesPagados() { return mesesPagados; }
    public void setMesesPagados(int mesesPagados) { this.mesesPagados = mesesPagados; }
    public double getTotalPagado() { return totalPagado; }
    public void setTotalPagado(double totalPagado) { this.totalPagado = totalPagado; }
    public double getTotalNoPagado() { return totalNoPagado; }
    public void setTotalNoPagado(double totalNoPagado) { this.totalNoPagado = totalNoPagado; }
    public List<ResultadoPagoRetenidos> getDetalle() { return detalle; }
    public void setDetalle(List<ResultadoPagoRetenidos> detalle) { this.detalle = detalle; }
    public List<String> getErrores() { return errores; }
    public void setErrores(List<String> errores) { this.errores = errores; }
}
