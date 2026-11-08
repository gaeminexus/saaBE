package com.saa.ejb.crd.service.dto;

/**
 * Cuerpo de {@code POST /prst/evaluarOtorgamiento}: evalúa un crédito que todavía no existe (simulador). La
 * {@code cuotaNueva} es la cuota TOTAL (con desgravamen y seguros) de la primera cuota de la tabla simulada.
 */
public class SolicitudEvaluacionOtorgamiento {

    private Long idEntidad;
    private Double monto;
    private Double cuotaNueva;
    private Long idPrestamoOriginal;

    public Long getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }
    public Double getCuotaNueva() { return cuotaNueva; }
    public void setCuotaNueva(Double cuotaNueva) { this.cuotaNueva = cuotaNueva; }
    public Long getIdPrestamoOriginal() { return idPrestamoOriginal; }
    public void setIdPrestamoOriginal(Long idPrestamoOriginal) { this.idPrestamoOriginal = idPrestamoOriginal; }
}
