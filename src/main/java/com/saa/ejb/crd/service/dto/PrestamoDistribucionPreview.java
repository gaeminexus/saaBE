package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/** Un préstamo dentro de {@code PreviewDistribucionSeguro} (contrato §5). */
public class PrestamoDistribucionPreview {

    private Long idPrestamo;
    private String numeroPrestamo;
    private double base;
    private long mesesCubiertos;
    private double peso;
    private double valorAsignado;
    private List<CuotaDistribucionPreview> cuotas = new ArrayList<>();

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public String getNumeroPrestamo() { return numeroPrestamo; }
    public void setNumeroPrestamo(String numeroPrestamo) { this.numeroPrestamo = numeroPrestamo; }

    public double getBase() { return base; }
    public void setBase(double base) { this.base = base; }

    public long getMesesCubiertos() { return mesesCubiertos; }
    public void setMesesCubiertos(long mesesCubiertos) { this.mesesCubiertos = mesesCubiertos; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public double getValorAsignado() { return valorAsignado; }
    public void setValorAsignado(double valorAsignado) { this.valorAsignado = valorAsignado; }

    public List<CuotaDistribucionPreview> getCuotas() { return cuotas; }
    public void setCuotas(List<CuotaDistribucionPreview> cuotas) { this.cuotas = cuotas; }
}
