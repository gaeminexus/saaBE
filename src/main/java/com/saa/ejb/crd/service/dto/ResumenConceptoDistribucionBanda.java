package com.saa.ejb.crd.service.dto;

/** Un renglón del resumen por concepto dentro de {@link ResultadoDetalleDistribucionBanda}. */
public class ResumenConceptoDistribucionBanda {

    private String concepto;
    private double valor;
    private long filas;

    public ResumenConceptoDistribucionBanda() {
    }

    public ResumenConceptoDistribucionBanda(String concepto, double valor, long filas) {
        this.concepto = concepto;
        this.valor = valor;
        this.filas = filas;
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
}
