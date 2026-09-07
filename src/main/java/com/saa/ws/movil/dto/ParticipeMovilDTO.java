package com.saa.ws.movil.dto;

/**
 * Perfil consolidado del partícipe para {@code GET /movil/participe/{idEntidad}}. Recortado a
 * propósito (§5.2 del contrato): identificación, nombres, apellidos y contacto — nada de columnas
 * internas de {@code CRD.ENTD} (auditoría, migración, búsqueda, IPs, etc.).
 */
public class ParticipeMovilDTO {

    private Long idEntidad;
    private String identificacion;
    private String nombres;
    private String apellidos;
    private String correoPersonal;
    private String correoInstitucional;
    private String telefono;
    private String movil;

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

    public String getCorreoPersonal() {
        return correoPersonal;
    }

    public void setCorreoPersonal(String correoPersonal) {
        this.correoPersonal = correoPersonal;
    }

    public String getCorreoInstitucional() {
        return correoInstitucional;
    }

    public void setCorreoInstitucional(String correoInstitucional) {
        this.correoInstitucional = correoInstitucional;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getMovil() {
        return movil;
    }

    public void setMovil(String movil) {
        this.movil = movil;
    }
}
