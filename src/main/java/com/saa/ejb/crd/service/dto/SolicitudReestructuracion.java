package com.saa.ejb.crd.service.dto;

/**
 * Cuerpo de {@code POST /rest/prst/simularReestructuracion}. Las cuatro palancas de la decisión
 * 2 de PLAN-SIMULADORES-PRESTAMOS.md §4, combinables.
 */
public class SolicitudReestructuracion {

    private Long idPrestamo;

    /** true = suma mora + interés vencido pendientes al capital de arranque de la tabla nueva. */
    private Boolean capitalizarVencido;

    /** null = mantener la tasa actual del préstamo (PRSTTSAA). */
    private Double nuevaTasaAnual;

    /** null = mantener el plazo actual (la cantidad de cuotas pendientes hoy). */
    private Integer nuevoPlazo;

    /** 0 = sin gracia. La calculadora solo soporta 0 o 1 (un único período de gracia). */
    private Integer mesesGracia;

    public SolicitudReestructuracion() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Boolean getCapitalizarVencido() {
        return capitalizarVencido;
    }

    public void setCapitalizarVencido(Boolean capitalizarVencido) {
        this.capitalizarVencido = capitalizarVencido;
    }

    public Double getNuevaTasaAnual() {
        return nuevaTasaAnual;
    }

    public void setNuevaTasaAnual(Double nuevaTasaAnual) {
        this.nuevaTasaAnual = nuevaTasaAnual;
    }

    public Integer getNuevoPlazo() {
        return nuevoPlazo;
    }

    public void setNuevoPlazo(Integer nuevoPlazo) {
        this.nuevoPlazo = nuevoPlazo;
    }

    public Integer getMesesGracia() {
        return mesesGracia;
    }

    public void setMesesGracia(Integer mesesGracia) {
        this.mesesGracia = mesesGracia;
    }
}
