package com.saa.ejb.crd.service.dto;

/**
 * Cuerpo genérico {@code {usuario, motivo}} — {@code POST /posg/{id}/distribuir} (sin motivo) y
 * {@code POST /posg/{id}/anular} (con motivo obligatorio), contrato §5 y §6.
 */
public class SolicitudUsuarioMotivo {

    private String usuario;
    private String motivo;

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
