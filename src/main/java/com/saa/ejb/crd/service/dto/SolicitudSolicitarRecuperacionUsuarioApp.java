package com.saa.ejb.crd.service.dto;

/** Body de {@code POST /movil/auth/solicitarRecuperacion} — "olvidé mi clave", paso 1 (cédula). */
public class SolicitudSolicitarRecuperacionUsuarioApp {

    private String identificacion;

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }
}
