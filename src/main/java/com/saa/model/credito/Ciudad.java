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
 * Representa la tabla CDDD (Ciudad).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CDDD", schema = "CRD")
@SequenceGenerator(name = "SQ_CDDDCDGO", sequenceName = "CRD.SQ_CDDDCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "CiudadAll", query = "select e from Ciudad e"),
    @NamedQuery(name = "CiudadId", query = "select e from Ciudad e where e.codigo = :id")
})
public class Ciudad implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "CDDDCDGO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CDDDCDGO")
    private Long codigo;

    /**
     * FK - Código Provincia.
     */
    @ManyToOne
    @JoinColumn(name = "PRVNCDGO", referencedColumnName = "PRVNCDGO")
    private Provincia provincia;

    /**
     * Nombre de la ciudad.
     */
    @Basic
    @Column(name = "CDDDNMBR", length = 2000)
    private String nombre;

    /**
     * Código alterno.
     */
    @Basic
    @Column(name = "CDDDCDAL", length = 50)
    private String codigoAlterno;

    /**
     * Fecha de ingreso.
     */
    @Basic
    @Column(name = "CDDDFCIN")
    private Timestamp fechaIngreso;

    /**
     * Usuario de ingreso.
     */
    @Basic
    @Column(name = "CDDDUSIN", length = 50)
    private String usuarioIngreso;

    /**
     * Código externo.
     */
    @Basic
    @Column(name = "CDDDCDEX", length = 50)
    private String codigoExterno;

    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "CDDDIDST")
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

    public Provincia getProvincia() {
        return provincia;
    }

    public void setProvincia(Provincia provincia) {
        this.provincia = provincia;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigoAlterno() {
        return codigoAlterno;
    }

    public void setCodigoAlterno(String codigoAlterno) {
        this.codigoAlterno = codigoAlterno;
    }

    public Timestamp getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Timestamp fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getUsuarioIngreso() {
        return usuarioIngreso;
    }

    public void setUsuarioIngreso(String usuarioIngreso) {
        this.usuarioIngreso = usuarioIngreso;
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
