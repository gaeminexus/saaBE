package com.saa.ejb.crd.service.dto;

/**
 * Una línea de la escala de calificación de riesgo dentro de {@link SolicitudConfiguracionCalificacionRiesgo}
 * / {@link SolicitudCierreVigenciaCalificacion} — PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md, P22.
 *
 * <b>{@code diaDesde} y {@code orden} NO se envían — el servicio los deriva.</b> El orden de la
 * lista ES el orden de evaluación (de ahí sale {@code orden}), y {@code diaDesde} de cada línea es
 * siempre {@code diaHasta} de la anterior más uno, empezando en 0 la primera (decisión del árbitro
 * `omen-saa-1-arb`, 2026-09-07, mismo razonamiento que ya aplicaba a {@code orden}: un dato que
 * puede contradecir a otro y no es la fuente de verdad, no se recibe — acá {@code diaDesde} es
 * enteramente derivable de {@code diaHasta} más la posición, así que aceptarlo del cliente sólo
 * abriría la puerta a un hueco o un solape que la validación tendría que perseguir en vez de hacer
 * estructuralmente imposible).
 */
public class SolicitudEscala {

    /** A1, A2, A3, B1, B2, C1, C2, D, E. */
    private String calificacion;

    /** Día hasta, inclusive. {@code null} SOLO en la última línea (sin tope superior). */
    private Long diaHasta;

    /** Tanto por uno (0.0099 = 0,99%). Entre 0 y 1. */
    private Double porcentajeProvision;

    public SolicitudEscala() {
    }

    public String getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(String calificacion) {
        this.calificacion = calificacion;
    }

    public Long getDiaHasta() {
        return diaHasta;
    }

    public void setDiaHasta(Long diaHasta) {
        this.diaHasta = diaHasta;
    }

    public Double getPorcentajeProvision() {
        return porcentajeProvision;
    }

    public void setPorcentajeProvision(Double porcentajeProvision) {
        this.porcentajeProvision = porcentajeProvision;
    }
}
