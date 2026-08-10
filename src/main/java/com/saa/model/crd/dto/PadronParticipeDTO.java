package com.saa.model.crd.dto;

import java.io.Serializable;

/**
 * DTO para el padrón de partícipes (habilitación de voto y elegibilidad).
 * Los datos del partícipe provienen de CRD.ENTD, el nombre de la calidad de
 * CRD.ESPR, y los indicadores de aportes y mora de CRD.APRT (tipos 9 =
 * jubilación, 11 = cesantía).
 */
public class PadronParticipeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Número secuencial de fila del padrón (columna "No."). */
    private Long numero;

    /** Código de la entidad (ENTDCDGO), para trazabilidad contra la base. */
    private Long entidadId;

    /** Cédula de identidad (ENTDNMID). */
    private String cedula;

    /** Nombres y apellidos (ENTDRZNS). */
    private String nombresApellidos;

    /** Código de la calidad del partícipe (ENTDIDST = ESPRCDGO). */
    private Long calidadParticipeId;

    /** Nombre de la calidad del partícipe (ESPRNMBR): ACTIVO / CESANTE / ... */
    private String calidadParticipe;

    /** Meses distintos con al menos un aporte positivo de tipo 9 u 11. */
    private Long numeroAportes;

    /** "AL DIA" o "EN MORA". */
    private String estadoMora;

    /** Meses en mora; 0 si está al día, null si nunca registró un aporte. */
    private Long mesesEnMora;

    /** "SI" o "NO". */
    private String habilitadoVoto;

    /** "SI" o "NO". */
    private String elegibleMiembro;

    public PadronParticipeDTO() {
    }

    public PadronParticipeDTO(Long numero, Long entidadId, String cedula, String nombresApellidos,
                              Long calidadParticipeId, String calidadParticipe, Long numeroAportes,
                              String estadoMora, Long mesesEnMora, String habilitadoVoto,
                              String elegibleMiembro) {
        this.numero = numero;
        this.entidadId = entidadId;
        this.cedula = cedula;
        this.nombresApellidos = nombresApellidos;
        this.calidadParticipeId = calidadParticipeId;
        this.calidadParticipe = calidadParticipe;
        this.numeroAportes = numeroAportes;
        this.estadoMora = estadoMora;
        this.mesesEnMora = mesesEnMora;
        this.habilitadoVoto = habilitadoVoto;
        this.elegibleMiembro = elegibleMiembro;
    }

    // Getters y Setters
    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombresApellidos() {
        return nombresApellidos;
    }

    public void setNombresApellidos(String nombresApellidos) {
        this.nombresApellidos = nombresApellidos;
    }

    public Long getCalidadParticipeId() {
        return calidadParticipeId;
    }

    public void setCalidadParticipeId(Long calidadParticipeId) {
        this.calidadParticipeId = calidadParticipeId;
    }

    public String getCalidadParticipe() {
        return calidadParticipe;
    }

    public void setCalidadParticipe(String calidadParticipe) {
        this.calidadParticipe = calidadParticipe;
    }

    public Long getNumeroAportes() {
        return numeroAportes;
    }

    public void setNumeroAportes(Long numeroAportes) {
        this.numeroAportes = numeroAportes;
    }

    public String getEstadoMora() {
        return estadoMora;
    }

    public void setEstadoMora(String estadoMora) {
        this.estadoMora = estadoMora;
    }

    public Long getMesesEnMora() {
        return mesesEnMora;
    }

    public void setMesesEnMora(Long mesesEnMora) {
        this.mesesEnMora = mesesEnMora;
    }

    public String getHabilitadoVoto() {
        return habilitadoVoto;
    }

    public void setHabilitadoVoto(String habilitadoVoto) {
        this.habilitadoVoto = habilitadoVoto;
    }

    public String getElegibleMiembro() {
        return elegibleMiembro;
    }

    public void setElegibleMiembro(String elegibleMiembro) {
        this.elegibleMiembro = elegibleMiembro;
    }
}
