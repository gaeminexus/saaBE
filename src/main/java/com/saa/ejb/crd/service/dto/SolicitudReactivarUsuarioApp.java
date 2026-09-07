package com.saa.ejb.crd.service.dto;

/**
 * Body de {@code POST /rest/usap/reactivar} — oficina revierte el borrado (ELIMINADO) de
 * una cuenta. Endpoint de intranet: NUNCA lo consume SaaMovilBE.
 */
public class SolicitudReactivarUsuarioApp {

    private String identificacion;
    private String claveTemporal;
    /** Usuario de oficina que reactiva (obligatorio) — queda en CRD.USAP.USAPUSAR. */
    private String usuario;

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getClaveTemporal() {
        return claveTemporal;
    }

    public void setClaveTemporal(String claveTemporal) {
        this.claveTemporal = claveTemporal;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}
