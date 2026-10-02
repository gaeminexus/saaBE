package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/** Una exclusión de {@code GET /posg/{idFactura}/novedades} (contrato §7). */
public class ExclusionSeguro {

    private Long idPrestamo;
    private String numeroPrestamo;
    private String nombreParticipe;
    private String cedula;
    private double base;
    /** PRECANCELACION · ABONO_CAPITAL · PLAZO_VENCIDO. */
    private String motivo;
    private LocalDate fecha;

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public String getNumeroPrestamo() { return numeroPrestamo; }
    public void setNumeroPrestamo(String numeroPrestamo) { this.numeroPrestamo = numeroPrestamo; }

    public String getNombreParticipe() { return nombreParticipe; }
    public void setNombreParticipe(String nombreParticipe) { this.nombreParticipe = nombreParticipe; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public double getBase() { return base; }
    public void setBase(double base) { this.base = base; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
}
