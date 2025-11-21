package com.saa.model.credito;

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
 * Representa la tabla FLLL (Filial).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FLLL", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "FilialAll", query = "select e from Filial e"),
    @NamedQuery(name = "FilialId", query = "select e from Filial e where e.codigo = :id")
})
public class Filial implements Serializable {

    /**
     * Código de la filial.
     */
    @Id
    @Basic
    @Column(name = "FLLLCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Nombre de la filial.
     */
    @Basic
    @Column(name = "FLLLNMBR", length = 2000)
    private String nombre;

    /**
     * Código alterno.
     */
    @Basic
    @Column(name = "FLLLCDAL", length = 50)
    private String codigoAlterno;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "FLLLIDST")
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
     * Devuelve codigoAlterno.
     * @return codigoAlterno.
     */
    public String getCodigoAlterno() {
        return codigoAlterno;
    }

    /**
     * Asigna codigoAlterno.
     * @param codigoAlterno nuevo valor para codigoAlterno.
     */
    public void setCodigoAlterno(String codigoAlterno) {
        this.codigoAlterno = codigoAlterno;
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

