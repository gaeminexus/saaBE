package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

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
import jakarta.persistence.Table;

/**
 * Representa la tabla PRRQ (Parroquia).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRRQ", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ParroquiaAll", query = "select e from Parroquia e"),
    @NamedQuery(name = "ParroquiaId", query = "select e from Parroquia e where e.codigo = :id")
})
public class Parroquia implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "PRRQCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK - Código Ciudad.
     */
    @ManyToOne
    @JoinColumn(name = "CDDDCDGO", referencedColumnName = "CDDDCDGO")
    private Ciudad ciudad;

    /**
     * Nombre de la parroquia.
     */
    @Basic
    @Column(name = "PRRQNMBR", length = 2000)
    private String nombre;

    /**
     * Usuario de ingreso.
     */
    @Basic
    @Column(name = "PRRQUSIN", length = 50)
    private String usuarioIngreso;

    /**
     * Fecha de ingreso.
     */
    @Basic
    @Column(name = "PRRQFCIN")
    private LocalDateTime fechaIngreso;

    /**
     * Código externo.
     */
    @Basic
    @Column(name = "PRRQCDEX", length = 50)
    private String codigoExterno;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "PRRQIDST")
    private Long estado;

    // ======================
    // Getters y Setters
    // ======================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Ciudad getCiudad() {
        return ciudad;
    }

    public void setCiudad(Ciudad ciudad) {
        this.ciudad = ciudad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getCodigoExterno() {
        return codigoExterno;
    }

    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

