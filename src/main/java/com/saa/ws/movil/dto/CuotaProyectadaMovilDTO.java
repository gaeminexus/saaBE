package com.saa.ws.movil.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Espejo movil de {@code CuotaProyectada} (tabla de la simulación) — fecha como texto. */
public class CuotaProyectadaMovilDTO {

    private Double numeroCuota;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaVencimiento;

    private Double capital;
    private Double interes;
    private Double cuota;
    private Double saldoCapital;
    private Double desgravamen;
    private Double seguroIncendio;
    private Double total;

    public Double getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Double numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDateTime fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Double getCapital() {
        return capital;
    }

    public void setCapital(Double capital) {
        this.capital = capital;
    }

    public Double getInteres() {
        return interes;
    }

    public void setInteres(Double interes) {
        this.interes = interes;
    }

    public Double getCuota() {
        return cuota;
    }

    public void setCuota(Double cuota) {
        this.cuota = cuota;
    }

    public Double getSaldoCapital() {
        return saldoCapital;
    }

    public void setSaldoCapital(Double saldoCapital) {
        this.saldoCapital = saldoCapital;
    }

    public Double getDesgravamen() {
        return desgravamen;
    }

    public void setDesgravamen(Double desgravamen) {
        this.desgravamen = desgravamen;
    }

    public Double getSeguroIncendio() {
        return seguroIncendio;
    }

    public void setSeguroIncendio(Double seguroIncendio) {
        this.seguroIncendio = seguroIncendio;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}
