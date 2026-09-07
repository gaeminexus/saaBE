package com.saa.ws.movil.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/** Una cuota de {@code /movil/prestamos/{idEntidad}/{idPrestamo}/cuotas}, recortada de {@code DetallePrestamo}. */
public class CuotaPrestamoMovilDTO {

    private Long codigo;
    private Double numeroCuota;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaVencimiento;

    private Double capital;
    private Double interes;
    private Double mora;
    private Double cuota;
    private Double saldoCapital;
    private Double saldo;
    private Long estado;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaPagado;

    private Double capitalPagado;
    private Double interesPagado;
    private Long diasMora;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

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

    public Double getMora() {
        return mora;
    }

    public void setMora(Double mora) {
        this.mora = mora;
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

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaPagado() {
        return fechaPagado;
    }

    public void setFechaPagado(LocalDateTime fechaPagado) {
        this.fechaPagado = fechaPagado;
    }

    public Double getCapitalPagado() {
        return capitalPagado;
    }

    public void setCapitalPagado(Double capitalPagado) {
        this.capitalPagado = capitalPagado;
    }

    public Double getInteresPagado() {
        return interesPagado;
    }

    public void setInteresPagado(Double interesPagado) {
        this.interesPagado = interesPagado;
    }

    public Long getDiasMora() {
        return diasMora;
    }

    public void setDiasMora(Long diasMora) {
        this.diasMora = diasMora;
    }
}
