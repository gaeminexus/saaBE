package com.saa.ejb.crd.service.dto;

/**
 * Body de POST /rest/rvsg/{id}/aprobar, /rechazar y /anular. {@code motivo} es obligatorio en
 * rechazar y anular; en aprobar se ignora.
 */
public class SolicitudDecisionRecepcionSeguro {

    private String usuario;

    private String motivo;

    /** Se ignora: la empresa contable se deriva de la cuenta bancaria de la recepción. */
    private Long idEmpresa;

    public SolicitudDecisionRecepcionSeguro() {
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }
}
