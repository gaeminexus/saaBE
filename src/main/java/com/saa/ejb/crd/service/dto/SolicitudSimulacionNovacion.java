package com.saa.ejb.crd.service.dto;

/** Cuerpo de {@code POST /prst/simularNovacion}. No graba nada. */
public class SolicitudSimulacionNovacion {

    private Long idPrestamoOriginal;
    private Double monto;
    private Integer plazo;

    public Long getIdPrestamoOriginal() { return idPrestamoOriginal; }
    public void setIdPrestamoOriginal(Long idPrestamoOriginal) { this.idPrestamoOriginal = idPrestamoOriginal; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public Integer getPlazo() { return plazo; }
    public void setPlazo(Integer plazo) { this.plazo = plazo; }
}
