package com.saa.model.credito;

import java.io.Serializable;
import java.time.LocalDateTime;

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
 * Representa la tabla CXCK (CXCKardexParticipe).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CXCK", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CxcKardexParticipeAll", query = "select e from CxcKardexParticipe e"),
    @NamedQuery(name = "CxcKardexParticipeId", query = "select e from CxcKardexParticipe e where e.codigo = :id")
})
public class CxcKardexParticipe implements Serializable {

    /**
     * Código.
     */
    @Id
    @Basic
    @Column(name = "CXCKCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Cuenta por cobrar del partícipe.
     */
    @ManyToOne
    @JoinColumn(name = "CXCPCDGO", referencedColumnName = "CXCPCDGO")
    private CxcParticipe cxcpParticipe;

    /**
     * ID Transacción.
     */
    @Basic
    @Column(name = "CXCKIDTR")
    private Long idTransaccion;

    /**
     * ID Cuenta.
     */
    @Basic
    @Column(name = "CXCKIDCN")
    private Long idCuenta;

    /**
     * Total débito.
     */
    @Basic
    @Column(name = "CXCKTTDB")
    private Double totalDebito;

    /**
     * Total crédito.
     */
    @Basic
    @Column(name = "CXCKTTCR")
    private Double totalCredito;

    /**
     * Saldo actual.
     */
    @Basic
    @Column(name = "CXCKSLAC")
    private Double saldoActual;

    /**
     * Concepto.
     */
    @Basic
    @Column(name = "CXCKCNCP", length = 2000)
    private String concepto;

    /**
     * Fecha creado.
     */
    @Basic
    @Column(name = "CXCKFCCR")
    private LocalDateTime fechaCreado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public CxcParticipe getCxcp() {
        return cxcpParticipe;
    }

    public void setCxcp(CxcParticipe cxcp) {
        this.cxcpParticipe = cxcp;
    }

    public Long getIdTransaccion() {
        return idTransaccion;
    }

    public void setIdTransaccion(Long idTransaccion) {
        this.idTransaccion = idTransaccion;
    }

    public Long getIdCuenta() {
        return idCuenta;
    }

    public void setIdCuenta(Long idCuenta) {
        this.idCuenta = idCuenta;
    }

    public Double getTotalDebito() {
        return totalDebito;
    }

    public void setTotalDebito(Double totalDebito) {
        this.totalDebito = totalDebito;
    }

    public Double getTotalCredito() {
        return totalCredito;
    }

    public void setTotalCredito(Double totalCredito) {
        this.totalCredito = totalCredito;
    }

    public Double getSaldoActual() {
        return saldoActual;
    }

    public void setSaldoActual(Double saldoActual) {
        this.saldoActual = saldoActual;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public LocalDateTime getFechaCreado() {
        return fechaCreado;
    }

    public void setFechaCreado(LocalDateTime fechaCreado) {
        this.fechaCreado = fechaCreado;
    }
}

