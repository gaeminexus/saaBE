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
 * Representa la tabla BPRF (BioProfile).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "BPRF", schema = "CRD")

@NamedQueries({
    @NamedQuery(name = "BioProfileAll", query = "select e from BioProfile e"),
    @NamedQuery(name = "BioProfileId", query = "select e from BioProfile e where e.codigo = :id")
})
public class BioProfile implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "BPRFCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Identificación.
     */
    @Basic
    @Column(name = "BPRFIDNT", length = 50)
    private String identificacion;

    /**
     * ID de la entidad.
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /**
     * Fecha registro.
     */
    @Basic
    @Column(name = "BPRFFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "BPRFIDST")
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

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

