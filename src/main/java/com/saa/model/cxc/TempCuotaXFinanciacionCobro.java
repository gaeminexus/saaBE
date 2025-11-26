/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 * Este software es información confidencial y patentada de Compuseg Cía. Ltda.
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo
 * conforme los términos del acuerdo de licencia con Compuseg.
 */
package com.saa.model.cxc;

import java.io.Serializable;
import java.util.Date;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Pojo mapeo de tabla CBR.TCDC.
 * Entity TempCuotaXFinanciacionCobro.
 * Cuotas resultantes de la financiación de un documento de cobro.
 * Son hijas de la entidad TFDC (financiación por documento de cobro).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TCDC", schema = "CBR")
@SequenceGenerator(name = "SQ_TCDCCDGO", sequenceName = "CBR.SQ_TCDCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TempCuotaXFinanciacionCobroAll", query = "select e from TempCuotaXFinanciacionCobro e"),
    @NamedQuery(name = "TempCuotaXFinanciacionCobroId", query = "select e from TempCuotaXFinanciacionCobro e where e.codigo = :id")
})
public class TempCuotaXFinanciacionCobro implements Serializable {

    /**
     * Código de la entidad.
     */
    private Long codigo;

    /**
     * Financiación a la que pertenece la cuota.
     */
    private TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro;

    /**
     * Fecha de generación o ingreso de la cuota.
     */
    private Date fechaIngreso;

    /**
     * Fecha de vencimiento de la cuota.
     */
    private Date fechaVencimiento;

    /**
     * Tipo de cobro: 1 = cuota, 2 = letra, 3 = cuota inicial.
     */
    private Long tipo;

    /**
     * Valor a cobrar por la cuota.
     */
    private Double valor;

    /**
     * Número secuencial dentro de la financiación.
     */
    private Long numeroSecuencial;

    /**
     * Número de cuota o letra.
     */
    private Long numeroCuotaLetra;

    /**
     * Total de cuotas en la financiación.
     */
    private Long numeroTotalCuotas;

    /**
     * Total abonado a la cuota.
     */
    private Double totalAbono;

    /**
     * Saldo de la cuota.
     */
    private Double saldo;

    // ---------------------------------------------------
    // GETTERS Y SETTERS
    // ---------------------------------------------------

    @Id
    @Column(name = "TCDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCDCCDGO")
    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    @ManyToOne
    @JoinColumn(name = "TFDCCDGO", referencedColumnName = "TFDCCDGO")
    public TempFinanciacionXDocumentoCobro getTempFinanciacionXDocumentoCobro() {
        return tempFinanciacionXDocumentoCobro;
    }

    public void setTempFinanciacionXDocumentoCobro(
            TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro) {
        this.tempFinanciacionXDocumentoCobro = tempFinanciacionXDocumentoCobro;
    }

    @Basic
    @Column(name = "TCDCFCIN")
    @Temporal(TemporalType.TIMESTAMP)
    public Date getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Date fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    @Basic
    @Column(name = "TCDCFCVN")
    @Temporal(TemporalType.DATE)
    public Date getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(Date fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    @Basic
    @Column(name = "TCDCTPOO")
    public Long getTipo() {
        return tipo;
    }

    public void setTipo(Long tipo) {
        this.tipo = tipo;
    }

    @Basic
    @Column(name = "TCDCVLRR")
    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    @Basic
    @Column(name = "TCDCNMSC")
    public Long getNumeroSecuencial() {
        return numeroSecuencial;
    }

    public void setNumeroSecuencial(Long numeroSecuencial) {
        this.numeroSecuencial = numeroSecuencial;
    }

    @Basic
    @Column(name = "TCDCNMCL")
    public Long getNumeroCuotaLetra() {
        return numeroCuotaLetra;
    }

    public void setNumeroCuotaLetra(Long numeroCuotaLetra) {
        this.numeroCuotaLetra = numeroCuotaLetra;
    }

    @Basic
    @Column(name = "TCDCNMTC")
    public Long getNumeroTotalCuotas() {
        return numeroTotalCuotas;
    }

    public void setNumeroTotalCuotas(Long numeroTotalCuotas) {
        this.numeroTotalCuotas = numeroTotalCuotas;
    }

    @Basic
    @Column(name = "TCDCABNO")
    public Double getTotalAbono() {
        return totalAbono;
    }

    public void setTotalAbono(Double totalAbono) {
        this.totalAbono = totalAbono;
    }

    @Basic
    @Column(name = "TCDCSLDO")
    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }
}
