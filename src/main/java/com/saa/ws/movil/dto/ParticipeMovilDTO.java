package com.saa.ws.movil.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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

    /** Fecha (APRTFCTR) del aporte mas antiguo de la entidad; null si no tiene aportes. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaPrimerAporte;

    /** CRD.ENTD.ENTDIDST, que guarda el codigo alterno ESPRCDEX del catalogo CRD.ESPR (no su PK). */
    private Long idEstadoParticipe;

    /** ESPRNMBR del catalogo, resuelto por ESPRCDEX = ENTDIDST; null si el codigo no esta en el catalogo. */
    private String estadoParticipe;

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

    public LocalDateTime getFechaPrimerAporte() {
        return fechaPrimerAporte;
    }

    public void setFechaPrimerAporte(LocalDateTime fechaPrimerAporte) {
        this.fechaPrimerAporte = fechaPrimerAporte;
    }

    public Long getIdEstadoParticipe() {
        return idEstadoParticipe;
    }

    public void setIdEstadoParticipe(Long idEstadoParticipe) {
        this.idEstadoParticipe = idEstadoParticipe;
    }

    public String getEstadoParticipe() {
        return estadoParticipe;
    }

    public void setEstadoParticipe(String estadoParticipe) {
        this.estadoParticipe = estadoParticipe;
    }
}
