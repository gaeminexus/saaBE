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
 * Representa la tabla TPPR (Tipo de préstamo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPPR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "TipoPrestamoAll", query = "select e from TipoPrestamo e"),
    @NamedQuery(name = "TipoPrestamoId", query = "select e from TipoPrestamo e where e.codigo = :id")
})
public class TipoPrestamo implements Serializable {

    /**
     * Código del tipo de préstamo.
     */
    @Id
    @Basic
    @Column(name = "TPPRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Nombre del tipo de préstamo.
     */
    @Basic
    @Column(name = "TPPRNMBR", length = 2000)
    private String nombre;

    /**
     * Código SBS.
     */
    @Basic
    @Column(name = "TPPRCSPB", length = 50)
    private String codigoSBS;

    /**
     * Tipo.
     */
    @Basic
    @Column(name = "TPPRTPOO", length = 50)
    private String tipo;

    /**
     * Tasa aplicada.
     */
    @Basic
    @Column(name = "TPPRTSAA")
    private Long tasa;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "TPPRIDST")
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
     * Devuelve codigoSuperBancos.
     * @return codigoSuperBancos.
     */
    public String getCodigoSBS() {
        return codigoSBS;
    }

    /**
     * Asigna codigoSuperBancos.
     * @param codigoSuperBancos nuevo valor para codigoSuperBancos.
     */
    public void setCodigoSBS(String codigoSBS) {
        this.codigoSBS = codigoSBS;
    }

    /**
     * Devuelve tipo.
     * @return tipo.
     */
    public String getTipo() {
        return tipo;
    }

    /**
     * Asigna tipo.
     * @param tipo nuevo valor para tipo.
     */
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    /**
     * Devuelve tasa.
     * @return tasa.
     */
    public Long getTasa() {
        return tasa;
    }

    /**
     * Asigna tasa.
     * @param tasa nuevo valor para tasa.
     */
    public void setTasa(Long tasa) {
        this.tasa = tasa;
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
