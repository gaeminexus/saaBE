package com.saa.ejb.crd.service.dto;

/**
 * Respuesta de {@code POST /rest/asgn/reversarRecepcion/{idCarga}} — CONTRATO CONGELADO,
 * ver {@code docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md} §2.3.
 */
public class ResultadoReversarRecepcion {

    private Long idCarga;
    private Boolean confirmada;
    private Long idAsientoAnulado;
    private String mensaje;

    public Long getIdCarga() {
        return idCarga;
    }

    public void setIdCarga(Long idCarga) {
        this.idCarga = idCarga;
    }

    public Boolean getConfirmada() {
        return confirmada;
    }

    public void setConfirmada(Boolean confirmada) {
        this.confirmada = confirmada;
    }

    public Long getIdAsientoAnulado() {
        return idAsientoAnulado;
    }

    public void setIdAsientoAnulado(Long idAsientoAnulado) {
        this.idAsientoAnulado = idAsientoAnulado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
