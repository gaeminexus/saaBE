package com.saa.model.credito;

import java.io.Serializable;
import java.sql.Timestamp;

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
 * Representa la tabla CXCP (CXCParticipe).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CXCP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CxcParticipeAll", query = "select e from CxcParticipe e"),
    @NamedQuery(name = "CxcParticipeId", query = "select e from CxcParticipe e where e.codigo = :id")
})
public class CxcParticipe implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "CXCPCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Entidad (Participe).
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /**
     * ID Cliente.
     */
    @Basic
    @Column(name = "CXCPIDCL")
    private Long idCliente;

    /**
     * ID Cuenta.
     */
    @Basic
    @Column(name = "CXCPIDCN")
    private Long idCuenta;

    /**
     * Saldo de la cuenta.
     */
    @Basic
    @Column(name = "CXCPSLCN")
    private Long saldoCuenta;

    /**
     * Fecha de creación.
     */
    @Basic
    @Column(name = "CXCPFCHC")
    private Timestamp fechaCreacion;

    // ======================
    // Getters y Setters
    // ======================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public Long getIdCuenta() {
        return idCuenta;
    }

    public void setIdCuenta(Long idCuenta) {
        this.idCuenta = idCuenta;
    }

    public Long getSaldoCuenta() {
        return saldoCuenta;
    }

    public void setSaldoCuenta(Long saldoCuenta) {
        this.saldoCuenta = saldoCuenta;
    }

    public Timestamp getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Timestamp fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}

