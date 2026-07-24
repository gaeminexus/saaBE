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
 * Representa la tabla RRFF (ReferenciaFamiliar).
 * Almacena las referencias familiares de una entidad (partícipe).
 * Relación 1:N con ENTD.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RRFF", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ReferenciaFamiliarAll", query = "select e from ReferenciaFamiliar e"),
    @NamedQuery(name = "ReferenciaFamiliarId",  query = "select e from ReferenciaFamiliar e where e.codigo = :id")
})
public class ReferenciaFamiliar implements Serializable {

    /** Código PK */
    @Id
    @Basic
    @Column(name = "RRFFCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Entidad (Partícipe) */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** Nombres completos */
    @Basic
    @Column(name = "RRFFNMBR", length = 500)
    private String nombres;

    /** Cédula de identidad */
    @Basic
    @Column(name = "RRFFCDLA", length = 20)
    private String cedula;

    /** Número de contacto */
    @Basic
    @Column(name = "RRFFCNTC", length = 50)
    private String contacto;

    /** Parentesco */
    @Basic
    @Column(name = "RRFFPRNT", length = 200)
    private String parentesco;

    /** ID Estado */
    @Basic
    @Column(name = "RRFFIDST")
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

    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }

    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
}
