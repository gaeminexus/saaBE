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
 * Representa la tabla DRTR (DireccionTrabajo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DRTR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "DireccionTrabajoAll", query = "select e from DireccionTrabajo e"),
    @NamedQuery(name = "DireccionTrabajoId", query = "select e from DireccionTrabajo e where e.codigo = :id")
})
public class DireccionTrabajo implements Serializable {

    /**
     * Código de dirección de trabajo.
     */
    @Id
    @Basic
    @Column(name = "DRTRCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Dirección (FK).
     */
    @ManyToOne
    @JoinColumn(name = "DRCCCDGO", referencedColumnName = "DRCCCDGO")
    private Direccion direccion;

    /**
     * Entidad (trabajo).
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Direccion getDireccion() {
        return direccion;
    }

    public void setDireccion(Direccion direccion) {
        this.direccion = direccion;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }
}

