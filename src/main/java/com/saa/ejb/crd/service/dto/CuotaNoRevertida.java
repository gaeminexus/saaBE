package com.saa.ejb.crd.service.dto;

/**
 * Una cuota que la distribución tocó pero que YA se pagó después, así que
 * {@code POST /posg/{id}/anular} no la revierte (contrato §6) — se informa, no es error.
 */
public class CuotaNoRevertida {

    private Long idCuota;
    private Long idPrestamo;
    private String numeroPrestamo;
    private Double numeroCuota;

    public Long getIdCuota() { return idCuota; }
    public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public String getNumeroPrestamo() { return numeroPrestamo; }
    public void setNumeroPrestamo(String numeroPrestamo) { this.numeroPrestamo = numeroPrestamo; }

    public Double getNumeroCuota() { return numeroCuota; }
    public void setNumeroCuota(Double numeroCuota) { this.numeroCuota = numeroCuota; }
}
