package com.saa.model.credito;

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
 * Representa la tabla PRVN (Provincia).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRVN", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ProvinciaAll", query = "select e from Provincia e"),
    @NamedQuery(name = "ProvinciaId", query = "select e from Provincia e where e.codigo = :id")
})
public class Provincia implements Serializable {

    /**
     * Código de la provincia.
     */
    @Id
    @Basic
    @Column(name = "PRVNCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK - Código País.
     */
    @ManyToOne
    @JoinColumn(name = "PSSSCDGO", referencedColumnName = "PSSSCDGO")
    private Pais pais;

    /**
     * Nombre.
     */
    @Basic
    @Column(name = "PRVNNMBR", length = 2000)
    private String nombre;

    /**
     * Código alterno.
     */
    @Basic
    @Column(name = "PRVNCDAL", length = 50)
    private String codigoAlterno;

    /**
     * Código externo.
     */
    @Basic
    @Column(name = "PRVNCDEX", length = 50)
    private String codigoExterno;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "PRVNIDST")
    private Long estado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Pais getPais() {
        return pais;
    }

    public void setPais(Pais pais) {
        this.pais = pais;
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

