package com.saa.ejb.sri.service.dto;

import java.io.Serializable;

/**
 * Una línea de {@code air/detalleAir} de una compra del detalle del ATS (API-DETALLE-ATS.md §4.3).
 * Espejo de la retención de renta (AIR) que el generador escribe en {@code <detalleCompras>}.
 */
public class AirAts implements Serializable {

    private static final long serialVersionUID = 1L;

    private String codRetAir;
    private double baseImpAir;
    private double porcentajeAir;
    private double valRetAir;

    public String getCodRetAir() { return codRetAir; }
    public void setCodRetAir(String codRetAir) { this.codRetAir = codRetAir; }

    public double getBaseImpAir() { return baseImpAir; }
    public void setBaseImpAir(double baseImpAir) { this.baseImpAir = baseImpAir; }

    public double getPorcentajeAir() { return porcentajeAir; }
    public void setPorcentajeAir(double porcentajeAir) { this.porcentajeAir = porcentajeAir; }

    public double getValRetAir() { return valRetAir; }
    public void setValRetAir(double valRetAir) { this.valRetAir = valRetAir; }
}
