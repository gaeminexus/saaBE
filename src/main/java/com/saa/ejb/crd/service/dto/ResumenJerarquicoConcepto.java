package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Primer nivel de {@code resumenJerarquico} dentro de {@link ResultadoDetalleDistribucionBanda}
 * — API-AUDITORIA-BANDAS.md "Las DOS vistas". Agrupa por CONCEPTO, nunca por cuenta contable:
 * mora e interés ordinario comparten cuenta y se fusionarían si el primer nivel fuera la cuenta.
 */
public class ResumenJerarquicoConcepto {

    private String concepto;
    private double valor;
    private long filas;
    private List<ResumenJerarquicoCuentaBanda> detalle = new ArrayList<>();

    public ResumenJerarquicoConcepto() {
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public long getFilas() {
        return filas;
    }

    public void setFilas(long filas) {
        this.filas = filas;
    }

    public List<ResumenJerarquicoCuentaBanda> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<ResumenJerarquicoCuentaBanda> detalle) {
        this.detalle = detalle;
    }
}
