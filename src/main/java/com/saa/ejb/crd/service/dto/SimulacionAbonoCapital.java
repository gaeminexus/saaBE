package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de simular un abono a capital: cómo quedaría el préstamo SIN escribir nada.
 *
 * Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.3.
 */
public class SimulacionAbonoCapital {

    private Long idPrestamo;

    /** Capital pendiente hoy (suma del saldoCapital real de las cuotas pendientes) */
    private Double saldoCapitalActual;

    private Double valorAbono;

    /** 1 = mantiene la cuota y reduce el plazo; 2 = mantiene el plazo y reduce la cuota */
    private Integer modalidad;

    /** 1 = francesa, 2 = alemana (del préstamo) */
    private Long tipoAmortizacion;

    private Long plazoActual;
    private Long plazoNuevo;

    private Double cuotaActual;
    private Double cuotaNueva;

    /** Interés que el partícipe deja de pagar respecto de la tabla vigente */
    private Double ahorroIntereses;

    /** Cantidad de cuotas que serían reemplazadas (historizadas en CRD.HDTP) */
    private Integer cuotasAHistorizar;

    /**
     * Seguro de incendio TOTAL de las cuotas historizadas que NO tendrían cuota nueva
     * correspondiente (solo aplica en modalidad 1, cuando el plazo se acorta) — 0 si el plazo
     * no se acorta o si todas las historizadas tienen su cuota nueva. Insumo calculado para un
     * futuro proceso de reembolso a la aseguradora; ese proceso NO está implementado, esto solo
     * deja el dato disponible sin tener que recalcularlo.
     */
    private Double seguroIncendioLiberado;

    private List<CuotaProyectada> tablaProyectada = new ArrayList<>();

    public SimulacionAbonoCapital() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Double getSaldoCapitalActual() {
        return saldoCapitalActual;
    }

    public void setSaldoCapitalActual(Double saldoCapitalActual) {
        this.saldoCapitalActual = saldoCapitalActual;
    }

    public Double getValorAbono() {
        return valorAbono;
    }

    public void setValorAbono(Double valorAbono) {
        this.valorAbono = valorAbono;
    }

    public Integer getModalidad() {
        return modalidad;
    }

    public void setModalidad(Integer modalidad) {
        this.modalidad = modalidad;
    }

    public Long getTipoAmortizacion() {
        return tipoAmortizacion;
    }

    public void setTipoAmortizacion(Long tipoAmortizacion) {
        this.tipoAmortizacion = tipoAmortizacion;
    }

    public Long getPlazoActual() {
        return plazoActual;
    }

    public void setPlazoActual(Long plazoActual) {
        this.plazoActual = plazoActual;
    }

    public Long getPlazoNuevo() {
        return plazoNuevo;
    }

    public void setPlazoNuevo(Long plazoNuevo) {
        this.plazoNuevo = plazoNuevo;
    }

    public Double getCuotaActual() {
        return cuotaActual;
    }

    public void setCuotaActual(Double cuotaActual) {
        this.cuotaActual = cuotaActual;
    }

    public Double getCuotaNueva() {
        return cuotaNueva;
    }

    public void setCuotaNueva(Double cuotaNueva) {
        this.cuotaNueva = cuotaNueva;
    }

    public Double getAhorroIntereses() {
        return ahorroIntereses;
    }

    public void setAhorroIntereses(Double ahorroIntereses) {
        this.ahorroIntereses = ahorroIntereses;
    }

    public Integer getCuotasAHistorizar() {
        return cuotasAHistorizar;
    }

    public void setCuotasAHistorizar(Integer cuotasAHistorizar) {
        this.cuotasAHistorizar = cuotasAHistorizar;
    }

    public Double getSeguroIncendioLiberado() {
        return seguroIncendioLiberado;
    }

    public void setSeguroIncendioLiberado(Double seguroIncendioLiberado) {
        this.seguroIncendioLiberado = seguroIncendioLiberado;
    }

    public List<CuotaProyectada> getTablaProyectada() {
        return tablaProyectada;
    }

    public void setTablaProyectada(List<CuotaProyectada> tablaProyectada) {
        this.tablaProyectada = tablaProyectada;
    }
}
