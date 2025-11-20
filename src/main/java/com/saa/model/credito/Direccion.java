package com.saa.model.credito;

import java.io.Serializable;
import java.sql.Timestamp;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla DRCC (Direccion).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DRCC", schema = "CRD")
@SequenceGenerator(name = "SQ_DRCCCDGO", sequenceName = "CRD.SQ_DRCCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DireccionAll", query = "select e from Direccion e"),
    @NamedQuery(name = "DireccionId", query = "select e from Direccion e where e.codigo = :id")
})
public class Direccion implements Serializable {

    /** Código */
    @Id
    @Basic
    @Column(name = "DRCCCDGO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DRCCCDGO")
    private Long codigo;

    /** ID de la entidad */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** FK - Código Parroquia */
    @ManyToOne
    @JoinColumn(name = "PRRQCDGO", referencedColumnName = "PRRQCDGO")
    private Parroquia parroquia;

    /** Descripción Dirección */
    @Basic
    @Column(name = "DRCCDSCR", length = 2000)
    private String descripcion;

    /** Referencia */
    @Basic
    @Column(name = "DRCCRFRN", length = 2000)
    private String referencia;

    /** Teléfono */
    @Basic
    @Column(name = "DRCCTLFN", length = 20)
    private String telefono;

    /** Celular */
    @Basic
    @Column(name = "DRCCCLLR", length = 20)
    private String celular;

    /** Latitud */
    @Basic
    @Column(name = "DRCCLTTD")
    private Long latitud;

    /** Longitud */
    @Basic
    @Column(name = "DRCCLNGT")
    private Long longitud;

    /** Indica si es direccion por defecto */
    @Basic
    @Column(name = "DRCCPRDF")
    private Long porDefecto;

    /** Indica si es direccion de Trabajo */
    @Basic
    @Column(name = "DRCCTRBJ")
    private Long trabajo;

    /** Usuario ingreso */
    @Basic
    @Column(name = "DRCCUSIN", length = 50)
    private String usuarioIngreso;

    /** Fecha ingreso */
    @Basic
    @Column(name = "DRCCFCIN")
    private Timestamp fechaIngreso;

    /** ID Estado */
    @Basic
    @Column(name = "DRCCIDST")
    private Long estado;

    // ============================
    // Getters y Setters
    // ============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public Parroquia getParroquia() {
        return parroquia;
    }

    public void setParroquia(Parroquia parroquia) {
        this.parroquia = parroquia;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public Long getLatitud() {
        return latitud;
    }

    public void setLatitud(Long latitud) {
        this.latitud = latitud;
    }

    public Long getLongitud() {
        return longitud;
    }

    public void setLongitud(Long longitud) {
        this.longitud = longitud;
    }

    public Long getPorDefecto() {
        return porDefecto;
    }

    public void setPorDefecto(Long porDefecto) {
        this.porDefecto = porDefecto;
    }

    public Long getTrabajo() {
        return trabajo;
    }

    public void setTrabajo(Long trabajo) {
        this.trabajo = trabajo;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
    }

    public Timestamp getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Timestamp fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
