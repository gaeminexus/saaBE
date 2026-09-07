package com.saa.ejb.crd.service.dto;

/** Body de {@code POST /rest/usap/crear} — enrolamiento desde la oficina (FE de intranet). */
public class SolicitudCrearUsuarioApp {

    private String identificacion;
    private String claveTemporal;
    /** Usuario de oficina que enrola (obligatorio) — queda en CRD.USAP.USAPUSAR. */
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
