package com.saa.ejb.crd.service.dto;

/**
 * Cuerpo de los endpoints de aprobar/rechazar/reenviar un {@link com.saa.model.crd.CobroCredito}.
 * {@code motivo} solo aplica al rechazo.
 */
public class SolicitudAprobacionCobro {

    private String usuario;

    private String motivo;

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
}
