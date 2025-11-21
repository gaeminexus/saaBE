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
 * Representa la tabla TPPC (TipoParticipe).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPPC", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "TipoParticipeAll", query = "select e from TipoParticipe e"),
    @NamedQuery(name = "TipoParticipeId", query = "select e from TipoParticipe e where e.codigo = :id")
})
public class TipoParticipe implements Serializable {

    /**
     * Código del tipo de partícipe.
     */
    @Id
    @Basic
    @Column(name = "TPPCCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Nombre.
     */
    @Basic
    @Column(name = "TPPCNMBR", length = 2000)
    private String nombre;

    /**
     * Código Superintendencia de Bancos SBS.
     */
    @Basic
    @Column(name = "TPPCCSPB", length = 50)
    private String codigoSuperBancos;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "TPPCIDST")
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
     * Devuelve código Superintendencia de bancos SBS.
     * @return codigoSuperBancos.
     */
    public String getCodigoSuperBancos() {
        return codigoSuperBancos;
    }

    /**
     * Asigna código Superintendencia de bancos SBS.
     * @param codigoSuperBancos nuevo valor.
     */
    public void setCodigoSuperBancos(String codigoSuperBancos) {
        this.codigoSuperBancos = codigoSuperBancos;
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

