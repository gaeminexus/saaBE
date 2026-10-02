package com.saa.ejb.crd.service.dto;

/**
 * Un préstamo elegible para una póliza, con su base congelada o en vista previa — {@code GET
 * /posg/listado/preview} y las inclusiones de {@code GET /posg/{idFactura}/novedades} (contrato
 * §3 y §7).
 */
public class CandidatoSeguro {

    private Long idPrestamo;
    private String numeroPrestamo;
    private String nombreParticipe;
    private String cedula;
    private String tipoPrestamo;
    private Long estadoPrestamo;
    private double base;
    private boolean sinSumaAsegurada;

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public String getNumeroPrestamo() { return numeroPrestamo; }
    public void setNumeroPrestamo(String numeroPrestamo) { this.numeroPrestamo = numeroPrestamo; }

    public String getNombreParticipe() { return nombreParticipe; }
    public void setNombreParticipe(String nombreParticipe) { this.nombreParticipe = nombreParticipe; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getTipoPrestamo() { return tipoPrestamo; }
    public void setTipoPrestamo(String tipoPrestamo) { this.tipoPrestamo = tipoPrestamo; }

    public Long getEstadoPrestamo() { return estadoPrestamo; }
    public void setEstadoPrestamo(Long estadoPrestamo) { this.estadoPrestamo = estadoPrestamo; }

    public double getBase() { return base; }
    public void setBase(double base) { this.base = base; }

    public boolean isSinSumaAsegurada() { return sinSumaAsegurada; }
    public void setSinSumaAsegurada(boolean sinSumaAsegurada) { this.sinSumaAsegurada = sinSumaAsegurada; }
}
