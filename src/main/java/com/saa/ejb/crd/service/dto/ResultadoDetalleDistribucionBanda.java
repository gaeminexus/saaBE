package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/** Respuesta de {@code POST /rest/dsbn/detalle} — API-AUDITORIA-BANDAS.md §2. */
public class ResultadoDetalleDistribucionBanda {

    private long totalFilas;
    private int pagina;
    private int tamanio;
    /**
     * Suma del CONJUNTO FILTRADO COMPLETO, no de la página — corregido 2026-09-02 (defecto
     * real visto en pantalla: mostraba la suma de las 50 filas de la página junto al conteo de
     * las 3.448 del total, y el frontend lo usaba como denominador de % de participación,
     * dando porcentajes de 500%+). Se deriva del mismo GROUP BY sin paginar que arma
     * {@link #resumenJerarquico}, no de sumar {@link #filas}.
     */
    private double totalValorFiltrado;
    /**
     * ⚠️ Es la suma de esta PÁGINA nada más — pese al nombre, NO es un total. Para el total por
     * concepto sobre el conjunto filtrado completo usar {@link #resumenJerarquico} (primer
     * nivel), que además viene con cuenta contable/banda. Se mantiene por compatibilidad; no
     * usar como denominador de porcentajes ni como total mostrado en pantalla.
     */
    private List<ResumenConceptoDistribucionBanda> resumenPorConcepto = new ArrayList<>();
    private List<FilaDistribucionBanda> filas = new ArrayList<>();
    /**
     * Vista RESUMEN — API-AUDITORIA-BANDAS.md "Las DOS vistas" (2026-09-02). Calculado sobre el
     * conjunto FILTRADO COMPLETO (no sobre la página, a diferencia de {@link #resumenPorConcepto}
     * de arriba): concepto → cuenta contable/banda, con GROUP BY en la consulta.
     */
    private List<ResumenJerarquicoConcepto> resumenJerarquico = new ArrayList<>();

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

    public List<ResumenJerarquicoConcepto> getResumenJerarquico() {
        return resumenJerarquico;
    }

    public void setResumenJerarquico(List<ResumenJerarquicoConcepto> resumenJerarquico) {
        this.resumenJerarquico = resumenJerarquico;
    }
}
