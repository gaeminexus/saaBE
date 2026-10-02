package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/** Una cuota dentro de {@code PrestamoDistribucionPreview} (contrato §5). */
public class CuotaDistribucionPreview {

    private Long idCuota;
    private Double numeroCuota;
    private LocalDate fechaVencimiento;
    private double saldoInicialCapital;
    private double seguroAnterior;
    private double seguroNuevo;
    /** true si {@code DTPRSICP} vino nulo o ≤ 0: queda fuera del reparto interno (ÍTEM 0a). */
    private boolean sinPeso;

    public Long getIdCuota() { return idCuota; }
    public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }

    public Double getNumeroCuota() { return numeroCuota; }
    public void setNumeroCuota(Double numeroCuota) { this.numeroCuota = numeroCuota; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public double getSaldoInicialCapital() { return saldoInicialCapital; }
    public void setSaldoInicialCapital(double saldoInicialCapital) { this.saldoInicialCapital = saldoInicialCapital; }

    public double getSeguroAnterior() { return seguroAnterior; }
    public void setSeguroAnterior(double seguroAnterior) { this.seguroAnterior = seguroAnterior; }

    public double getSeguroNuevo() { return seguroNuevo; }
    public void setSeguroNuevo(double seguroNuevo) { this.seguroNuevo = seguroNuevo; }

    public boolean isSinPeso() { return sinPeso; }
    public void setSinPeso(boolean sinPeso) { this.sinPeso = sinPeso; }
}
