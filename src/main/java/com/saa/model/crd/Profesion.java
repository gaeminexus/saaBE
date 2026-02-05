package com.saa.model.crd;

import java.io.Serializable;

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
 * Representa la tabla PRFS (Catálogo de profesiones).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRFS", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ProfesionAll", query = "select e from Profesion e"),
    @NamedQuery(name = "ProfesionId", query = "select e from Profesion e where e.codigo = :id")
})
public class Profesion implements Serializable {

    /**
     * Código de la profesión.
     */
    @Id
    @Basic
    @Column(name = "PRFSCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Código Superintendencia de Bancos SBS.
     */
    @Basic
    @Column(name = "PRFSCSBC", length = 50)
    private String codigoSBS;

    /**
     * Nombre de la profesión.
     */
    @Basic
    @Column(name = "PRFSNMBR", length = 2000)
    private String nombre;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "PRFSIDST")
    private Long estado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    /**
     * Devuelve codigo.
     * @return codigo.
     */
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna codigo.
     * @param codigo nuevo valor para codigo.
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    /**
     * Devuelve codigoSBS.
     * @return codigoSBS.
     */
    public String getCodigoSBS() {
        return codigoSBS;
    }

    /**
     * Asigna codigoSBS.
     * @param codigoSBS nuevo valor para codigoSBS.
     */
    public void setCodigoSBS(String codigoSBS) {
        this.codigoSBS = codigoSBS;
    }

    /**
     * Devuelve nombre.
     * @return nombre.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Asigna nombre.
     * @param nombre nuevo valor para nombre.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Devuelve estado.
     * @return estado.
     */
    public Long getEstado() {
        return estado;
    }

    /**
     * Asigna estado.
     * @param estado nuevo valor para estado.
     */
    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

