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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla MTDP (Catálogo de métodos de pago).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MTDP", schema = "CRD")
@SequenceGenerator(name = "SQ_MTDPCDGO", sequenceName = "CRD.SQ_MTDPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "MetodoPagoAll", query = "select e from MetodoPago e"),
    @NamedQuery(name = "MetodoPagoId", query = "select e from MetodoPago e where e.codigo = :id")
})
public class MetodoPago implements Serializable {

    /**
     * Código del método de pago.
     */
    @Id
    @Basic
    @Column(name = "MTDPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_MTDPCDGO")
    private Long codigo;

    /**
     * Código Superintendencia de Bancos SBS.
     */
    @Basic
    @Column(name = "MTDPCSBC", length = 50)
    private String codigoSBS;

    /**
     * Nombre del método de pago.
     */
    @Basic
    @Column(name = "MTDPNMBR", length = 2000)
    private String nombre;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "MTDPIDST")
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
