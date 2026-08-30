package com.saa.ejb.crd.service.dto;

/**
 * Resultado de aplicar un {@link com.saa.model.crd.AcuerdoCondonacion} (pago + condonación +
 * CANCELADO, K1/K6/K8/K9).
 */
public class ResultadoAplicacionAcuerdo {

    private Long idAcuerdo;
    private Long idEvento;
    private Long idPrestamo;
    private Long estadoFinalPrestamo;
    private Double valorPagado;
    private Double valorCondonado;

    public Long getIdAcuerdo() {
        return idAcuerdo;
    }

    public void setIdAcuerdo(Long idAcuerdo) {
        this.idAcuerdo = idAcuerdo;
    }

    public Long getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Long idEvento) {
        this.idEvento = idEvento;
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getEstadoFinalPrestamo() {
        return estadoFinalPrestamo;
    }

    public void setEstadoFinalPrestamo(Long estadoFinalPrestamo) {
        this.estadoFinalPrestamo = estadoFinalPrestamo;
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
