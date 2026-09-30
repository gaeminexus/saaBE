package com.saa.ejb.crd.service.dto;

/** Cuerpo de {@code POST /rest/plvn/{id}/revertir} (§7 del contrato). */
public class SolicitudRevertirPlazoVencido {

    private String usuario;
    private String motivo;

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
