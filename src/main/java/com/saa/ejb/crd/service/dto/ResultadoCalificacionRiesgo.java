package com.saa.ejb.crd.service.dto;

/**
 * Resultado de {@code CalificacionRiesgoService#calificar}: la calificación (A1..E) y el
 * porcentaje de provisión que le corresponde a una antigüedad en días, según la escala
 * vigente del producto — PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md.
 */
public class ResultadoCalificacionRiesgo {

    private Long idConfiguracion;
    private Long idEscala;
    private String calificacion;
    private Double porcentajeProvision;
    private Long diaDesde;
    private Long diaHasta;

    public ResultadoCalificacionRiesgo() {
    }

    public Long getIdConfiguracion() {
        return idConfiguracion;
    }

    public void setIdConfiguracion(Long idConfiguracion) {
        this.idConfiguracion = idConfiguracion;
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

    public Double getPorcentajeProvision() {
        return porcentajeProvision;
    }

    public void setPorcentajeProvision(Double porcentajeProvision) {
        this.porcentajeProvision = porcentajeProvision;
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
}
