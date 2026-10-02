package com.saa.ejb.crd.service.dto;

/** Una fila de {@code GET /posg/{id}/prestamos} (contrato §8). */
public class PrestamoSeguroDTO {

    private Long idPrestamo;
    private String numeroPrestamo;
    private String nombreParticipe;
    private String cedula;
    private Long novedad;
    private double base;
    private Long mesesCubiertos;
    private Double peso;
    private Double valorAsignado;
    private Long cuotasRepartidas;

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public String getNumeroPrestamo() { return numeroPrestamo; }
    public void setNumeroPrestamo(String numeroPrestamo) { this.numeroPrestamo = numeroPrestamo; }

    public String getNombreParticipe() { return nombreParticipe; }
    public void setNombreParticipe(String nombreParticipe) { this.nombreParticipe = nombreParticipe; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public Long getNovedad() { return novedad; }
    public void setNovedad(Long novedad) { this.novedad = novedad; }

    public double getBase() { return base; }
    public void setBase(double base) { this.base = base; }

    public Long getMesesCubiertos() { return mesesCubiertos; }
    public void setMesesCubiertos(Long mesesCubiertos) { this.mesesCubiertos = mesesCubiertos; }

    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }

    public Double getValorAsignado() { return valorAsignado; }
    public void setValorAsignado(Double valorAsignado) { this.valorAsignado = valorAsignado; }

    public Long getCuotasRepartidas() { return cuotasRepartidas; }
    public void setCuotasRepartidas(Long cuotasRepartidas) { this.cuotasRepartidas = cuotasRepartidas; }
}
