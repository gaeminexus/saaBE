package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/** Respuesta de {@code POST /rest/dsbn/detalle} — API-AUDITORIA-BANDAS.md §2. */
public class ResultadoDetalleDistribucionBanda {

    private long totalFilas;
    private int pagina;
    private int tamanio;
    private double totalValorFiltrado;
    private List<ResumenConceptoDistribucionBanda> resumenPorConcepto = new ArrayList<>();
    private List<FilaDistribucionBanda> filas = new ArrayList<>();

    public ResultadoDetalleDistribucionBanda() {
    }

    public long getTotalFilas() {
        return totalFilas;
    }

    public void setTotalFilas(long totalFilas) {
        this.totalFilas = totalFilas;
    }

    public int getPagina() {
        return pagina;
    }

    public void setPagina(int pagina) {
        this.pagina = pagina;
    }

    public int getTamanio() {
        return tamanio;
    }

    public void setTamanio(int tamanio) {
        this.tamanio = tamanio;
    }

    public double getTotalValorFiltrado() {
        return totalValorFiltrado;
    }

    public void setTotalValorFiltrado(double totalValorFiltrado) {
        this.totalValorFiltrado = totalValorFiltrado;
    }

    public List<ResumenConceptoDistribucionBanda> getResumenPorConcepto() {
        return resumenPorConcepto;
    }

    public void setResumenPorConcepto(List<ResumenConceptoDistribucionBanda> resumenPorConcepto) {
        this.resumenPorConcepto = resumenPorConcepto;
    }

    public List<FilaDistribucionBanda> getFilas() {
        return filas;
    }

    public void setFilas(List<FilaDistribucionBanda> filas) {
        this.filas = filas;
    }
}
