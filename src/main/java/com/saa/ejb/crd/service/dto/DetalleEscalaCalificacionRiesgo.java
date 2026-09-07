package com.saa.ejb.crd.service.dto;

/**
 * Una línea de la escala de calificación de riesgo, para respuesta — P22. Mismo espíritu que
 * {@code BandaProductoDetalle}, con {@code etiqueta} ya armada ("0 - 30" / "mas de 450 (resto)")
 * para que el frontend no la calcule.
 */
public class DetalleEscalaCalificacionRiesgo {

    private Long idEscala;
    private String calificacion;
    private Long diaDesde;
    private Long diaHasta;
    private String etiqueta;
    private Double porcentajeProvision;

    public DetalleEscalaCalificacionRiesgo() {
    }

    public Long getIdEscala() {
        return idEscala;
    }

    public void setIdEscala(Long idEscala) {
        this.idEscala = idEscala;
    }

    public String getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(String calificacion) {
        this.calificacion = calificacion;
    }

    public Long getDiaDesde() {
        return diaDesde;
    }

    public void setDiaDesde(Long diaDesde) {
        this.diaDesde = diaDesde;
    }

    public Long getDiaHasta() {
        return diaHasta;
    }

    public void setDiaHasta(Long diaHasta) {
        this.diaHasta = diaHasta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public Double getPorcentajeProvision() {
        return porcentajeProvision;
    }

    public void setPorcentajeProvision(Double porcentajeProvision) {
        this.porcentajeProvision = porcentajeProvision;
    }
}
