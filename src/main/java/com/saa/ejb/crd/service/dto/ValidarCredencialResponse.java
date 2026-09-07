package com.saa.ejb.crd.service.dto;

/**
 * Respuesta de {@code POST /rest/usap/validarCredencial} — el contrato exacto que consume
 * el WAR de borde (SaaMovilBE). No cambiar los nombres de campo unilateralmente: la app
 * móvil ya integra contra esta forma.
 */
public class ValidarCredencialResponse {

    private Long idEntidad;
    private String identificacion;
    private String nombres;
    private String apellidos;
    private Boolean debeCambiarClave;

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public Boolean getDebeCambiarClave() {
        return debeCambiarClave;
    }

    public void setDebeCambiarClave(Boolean debeCambiarClave) {
        this.debeCambiarClave = debeCambiarClave;
    }
}
