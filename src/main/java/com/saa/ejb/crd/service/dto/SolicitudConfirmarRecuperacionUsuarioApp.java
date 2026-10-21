package com.saa.ejb.crd.service.dto;

/**
 * Body de {@code POST /movil/auth/confirmarRecuperacion} — "olvidé mi clave", paso 2.
 * {@code codigo} es TEXTO, no número: un código como "048213" no puede perder el cero.
 */
public class SolicitudConfirmarRecuperacionUsuarioApp {

    private String identificacion;
    private String codigo;
    private String claveNueva;

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getClaveNueva() {
        return claveNueva;
    }

    public void setClaveNueva(String claveNueva) {
        this.claveNueva = claveNueva;
    }
}
