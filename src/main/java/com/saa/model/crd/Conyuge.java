package com.saa.model.crd;

import java.io.Serializable;

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
 * Representa la tabla CNYG (Conyuge).
 * Almacena los datos del cónyuge de una entidad (partícipe).
 * Relación 1:1 con ENTD.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNYG", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ConyugeAll", query = "select e from Conyuge e"),
    @NamedQuery(name = "ConyugeId",  query = "select e from Conyuge e where e.codigo = :id")
})
public class Conyuge implements Serializable {

    /** Código PK */
    @Id
    @Basic
    @Column(name = "CNYGCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Entidad (Partícipe) */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** Nombres completos del cónyuge */
    @Basic
    @Column(name = "CNYGNMBR", length = 500)
    private String nombres;

    /** Número de cédula del cónyuge */
    @Basic
    @Column(name = "CNYGCDLA", length = 20)
    private String cedula;

    /** Correo electrónico del cónyuge */
    @Basic
    @Column(name = "CNYGCRREO", length = 500)
    private String correo;

    /** ID Estado */
    @Basic
    @Column(name = "CNYGIDST")
    private Long estado;

    // ============================
    // Getters y Setters
    // ============================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Entidad getEntidad() { return entidad; }
    public void setEntidad(Entidad entidad) { this.entidad = entidad; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
}
