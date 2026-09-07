package com.saa.ejb.crd.service.dto;

/** Body de {@code POST /rest/usap/cambiarClave}. */
public class SolicitudCambiarClaveUsuarioApp {

    private String identificacion;
    private String claveActual;
    private String claveNueva;

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getClaveActual() {
        return claveActual;
    }

    public void setClaveActual(String claveActual) {
        this.claveActual = claveActual;
    }

    public String getClaveNueva() {
        return claveNueva;
    }

    public void setClaveNueva(String claveNueva) {
        this.claveNueva = claveNueva;
    }
}
