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
 * Representa la tabla TPCL (Catálogo de tipos de calificación de crédito).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPCL", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "TipoCalificacionCreditoAll", query = "select e from TipoCalificacionCredito e"),
    @NamedQuery(name = "TipoCalificacionCreditoId", query = "select e from TipoCalificacionCredito e where e.codigo = :id")
})
public class TipoCalificacionCredito implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "TPCLCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Código Superintendencia de Bancos SBS.
     */
    @Basic
    @Column(name = "TPCLCSPB", length = 50)
    private String codigoSBS;

    /**
     * Nombre.
     */
    @Basic
    @Column(name = "TPCLNMBR", length = 2000)
    private String nombre;

    /**
     * Categoría.
     */
    @Basic
    @Column(name = "TPCLCTGR", length = 2000)
    private String categoria;

    /**
     * Provisión.
     */
    @Basic
    @Column(name = "TPCLPRVS")
    private Double provision;

    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "TPCLIDST")
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
     * Devuelve categoria.
     * @return categoria.
     */
    public String getCategoria() {
        return categoria;
    }

    /**
     * Asigna categoria.
     * @param categoria nuevo valor para categoria.
     */
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    /**
     * Devuelve provision.
     * @return provision.
     */
    public Double getProvision() {
        return provision;
    }

    /**
     * Asigna provision.
     * @param provision nuevo valor para provision.
     */
    public void setProvision(Double provision) {
        this.provision = provision;
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
