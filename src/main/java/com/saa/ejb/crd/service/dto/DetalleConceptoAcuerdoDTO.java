package com.saa.ejb.crd.service.dto;

/**
 * Una línea de {@link SolicitudRegistroAcuerdo}: un concepto del préstamo
 * ({@link com.saa.rubros.CrdConceptoPrestamo}) con su adeudado/pagado/condonado.
 */
public class DetalleConceptoAcuerdoDTO {

    /** Ver {@link com.saa.rubros.CrdConceptoPrestamo}. Obligatorio. */
    private Long concepto;

    /** Monto adeudado de este concepto al momento del registro. Obligatorio, >= 0. */
    private Double valorAdeudado;

    /** Monto que el socio paga de este concepto. Obligatorio, >= 0. */
    private Double valorPagado;

    /** Monto que se condona de este concepto. Obligatorio, >= 0; SIEMPRE 0 en Desgravamen y Seguro de incendio (K3). */
    private Double valorCondonado;

    public Long getConcepto() {
        return concepto;
    }

    public void setConcepto(Long concepto) {
        this.concepto = concepto;
    }

    public Double getValorAdeudado() {
        return valorAdeudado;
    }

    public void setValorAdeudado(Double valorAdeudado) {
        this.valorAdeudado = valorAdeudado;
    }

    public Double getValorPagado() {
        return valorPagado;
    }

    public void setValorPagado(Double valorPagado) {
        this.valorPagado = valorPagado;
    }

    public Double getValorCondonado() {
        return valorCondonado;
    }

    public void setValorCondonado(Double valorCondonado) {
        this.valorCondonado = valorCondonado;
    }
}
