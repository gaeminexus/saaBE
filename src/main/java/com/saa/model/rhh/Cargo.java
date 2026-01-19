package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Catálogo de cargos / puestos.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRGO", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "Cargo.findAll", query = "select c from Cargo c"),
    @NamedQuery(name = "Cargo.findById", query = "select c from Cargo c where c.codigo = :id"),
    @NamedQuery(name = "Cargo.findByNombre", query = "select c from Cargo c where c.nombre = :nombre")
})
public class Cargo implements Serializable {

    /**
     * Código único del cargo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CRGOCDGO")
    private Long codigo;

    /**
     * Nombre del cargo.
     */
    @Basic
    @Column(name = "CRGONMBR")
    private String nombre;

    /**
     * Descripción general del cargo.
     */
    @Basic
    @Column(name = "CRGODSCR")
    private String descripcion;

    /**
     * Requisitos del cargo.
     */
    @Basic
    @Column(name = "CRGORQST")
    private String requisitos;

    /**
     * Estado del registro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "CRGOESTD")
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CRGOFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "CRGOUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getRequisitos() {
        return requisitos;
    }

    public void setRequisitos(String requisitos) {
        this.requisitos = requisitos;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
