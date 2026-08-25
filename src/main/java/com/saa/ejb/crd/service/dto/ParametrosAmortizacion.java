package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Entrada de {@code CalculadoraAmortizacionService.calcular}. Escalares puros: a propósito NO
 * recibe una entidad {@code Prestamo}, para poder simular una tabla de amortización sin que el
 * préstamo exista todavía (PLAN-SIMULADORES-PRESTAMOS.md §6).
 */
public class ParametrosAmortizacion {

    private Double monto;
    private Double tasaAnual;
    private Integer plazo;

    /** 1 = Francesa, 2 = Alemana (com.saa.rubros.TipoAmortizacion, aún literal en el resto del código). */
    private Long tipoAmortizacion;

    private LocalDateTime fechaInicio;
    private Boolean tieneCuotaCero;

    /** Se suma tal cual al total de cada cuota regular; no participa en el cálculo de capital/interés. */
    private Double desgravamenPorCuota;

    /** Se suma tal cual al total de cada cuota regular; no participa en el cálculo de capital/interés. */
    private Double seguroIncendioPorCuota;

    public ParametrosAmortizacion() {
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public Double getTasaAnual() {
        return tasaAnual;
    }

    public void setTasaAnual(Double tasaAnual) {
        this.tasaAnual = tasaAnual;
    }

    public Integer getPlazo() {
        return plazo;
    }

    public void setPlazo(Integer plazo) {
        this.plazo = plazo;
    }

    public Long getTipoAmortizacion() {
        return tipoAmortizacion;
    }

    public void setTipoAmortizacion(Long tipoAmortizacion) {
        this.tipoAmortizacion = tipoAmortizacion;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Boolean getTieneCuotaCero() {
        return tieneCuotaCero;
    }

    public void setTieneCuotaCero(Boolean tieneCuotaCero) {
        this.tieneCuotaCero = tieneCuotaCero;
    }

    public Double getDesgravamenPorCuota() {
        return desgravamenPorCuota;
    }

    public void setDesgravamenPorCuota(Double desgravamenPorCuota) {
        this.desgravamenPorCuota = desgravamenPorCuota;
    }

    public Double getSeguroIncendioPorCuota() {
        return seguroIncendioPorCuota;
    }

    public void setSeguroIncendioPorCuota(Double seguroIncendioPorCuota) {
        this.seguroIncendioPorCuota = seguroIncendioPorCuota;
    }
}
