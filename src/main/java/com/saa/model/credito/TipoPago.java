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
 * Representa la tabla TPPG (Tipos de pago).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPPG", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "TipoPagoAll", query = "select e from TipoPago e"),
    @NamedQuery(name = "TipoPagoId", query = "select e from TipoPago e where e.codigo = :id")
})
public class TipoPago implements Serializable {

    /**
     * Código del tipo de pago.
     */
    @Id
    @Basic
    @Column(name = "TPPGCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Nombre del tipo de pago.
     */
    @Basic
    @Column(name = "TPPGNMBR", length = 2000)
    private String nombre;

    /**
     * Código Superintendencia de bancos (SBS).
     */
    @Basic
    @Column(name = "TPPGCSPB", length = 50)
    private String codigoSbs;

    /**
     * Tipo.
     */
    @Basic
    @Column(name = "TPPGTPOO", length = 10)
    private String tipo;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "TPPGOBSR", length = 2000)
    private String observacion;

    /**
     * ID Estado.
     */
    @Basic
    @Column(name = "TPPGIDST")
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
     * Devuelve codigoSbs.
     * @return codigoSbs.
     */
    public String getCodigoSbs() {
        return codigoSbs;
    }

    /**
     * Asigna codigoSbs.
     * @param codigoSbs nuevo valor para codigoSbs.
     */
    public void setCodigoSbs(String codigoSbs) {
        this.codigoSbs = codigoSbs;
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
     * Devuelve observacion.
     * @return observacion.
     */
    public String getObservacion() {
        return observacion;
    }

    /**
     * Asigna observacion.
     * @param observacion nuevo valor para observacion.
     */
    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    /**
     * Devuelve idEstado.
     * @return idEstado.
     */
    public Long getEstado() {
        return estado;
    }

    /**
     * Asigna idEstado.
     * @param idEstado nuevo valor para idEstado.
     */
    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

