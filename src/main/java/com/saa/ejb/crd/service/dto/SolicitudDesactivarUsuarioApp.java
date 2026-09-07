package com.saa.ejb.crd.service.dto;

/** Body de {@code POST /rest/usap/desactivar} — "eliminación de cuenta" exigida por las tiendas. */
public class SolicitudDesactivarUsuarioApp {

    private String identificacion;
    private String clave;

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}
