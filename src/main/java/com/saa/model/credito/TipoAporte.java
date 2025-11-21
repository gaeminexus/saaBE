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
 * Representa la tabla TPAP (Catálogo de tipos de aporte).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPAP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "TipoAporteAll", query = "select e from TipoAporte e"),
    @NamedQuery(name = "TipoAporteId", query = "select e from TipoAporte e where e.codigo = :id")
})
public class TipoAporte implements Serializable {

    /**
     * Código del tipo de aporte.
     */
    @Id
    @Basic
    @Column(name = "TPAPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Nombre del tipo de aporte.
     */
    @Basic
    @Column(name = "TPAPNMBR", length = 2000)
    private String nombre;

    /**
     * Código Superintendencia de Bancos SBS.
     */
    @Basic
    @Column(name = "TPAPCSBC", length = 50)
    private String codigoSBS;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "TPAPIDST")
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

