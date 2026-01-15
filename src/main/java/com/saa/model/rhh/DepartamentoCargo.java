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
 * Catálogo de departamentos o áreas de la institución.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DPRT", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "Departamento.findAll", query = "select d from Departamento d"),
    @NamedQuery(name = "Departamento.findById", query = "select d from Departamento d where d.codigo = :id"),
    @NamedQuery(name = "Departamento.findByNombre", query = "select d from Departamento d where d.nombre = :nombre"),
    @NamedQuery(name = "Departamento.findActivos", query = "select d from Departamento d where d.estado = 'A'")
})
public class DepartamentoCargo implements Serializable {

    /**
     * Código único del departamento.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DPRTCDGO")
    private Long codigo;

    /**
     * Nombre del departamento.
     */
    @Basic
    @Column(name = "DPRTNMBR", length = 120, nullable = false)
    private String nombre;

    /**
     * Estado del registro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "DPRTESTD", length = 1, nullable = false)
    private String estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "DPRTFCHR", nullable = false)
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "DPRTUSRR", length = 60)
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
