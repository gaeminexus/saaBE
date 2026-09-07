package com.saa.ejb.crd.service.dto;

/** Body de {@code POST /rest/usap/validarCredencial}. */
public class SolicitudValidarCredencial {

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
