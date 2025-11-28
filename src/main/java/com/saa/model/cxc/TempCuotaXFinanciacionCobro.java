/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de Compuseg Cía. Ltda. ("Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
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
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.TCDC.
 * Entity TempCuotaXFinanciacionCobro.
 * Cuotas que se deben cancelar del documento de cobro. 
 * Son el resultado de la financiacion por lo que son hijas de la entidad de 
 * financiacion por documento de cobro TFDC.
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
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "TCDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCDCCDGO")
    private Long codigo;
    
    /**
     * Financiacion a la que pertenece la cuota inicial.
     */
    @ManyToOne
    @JoinColumn(name = "TFDCCDGO", referencedColumnName = "TFDCCDGO")
    private TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro;
    
    /**
     * Fecha de generacion o ingreso de la cuota.
     */
    @Basic
    @Column(name = "TCDCFCIN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaIngreso;
    
    /**
     * Fecha de vencimiento de la cuota.
     */
    @Basic
    @Column(name = "TCDCFCVN")
    @Temporal(TemporalType.DATE)
    private Date fechaVencimiento;
    
    /**
     * Tipo de cobro. 1 = cuota, 2 = letra, 3 = cuota inicial.
     */
    @Basic
    @Column(name = "TCDCTPOO")
    private Long tipo;
    
    /**
     * Valor a cobrar por la cuota.    
     */
    @Basic
    @Column(name = "TCDCVLRR")
    private Double valor;
    
    /**
     * Numero secuencial de cuota dentro del total de la financiación. 
     */
    @Basic
    @Column(name = "TCDCNMSC")
    private Long numeroSecuencial;
    
    /**
     * Numero de cuota o letra. el numero de letra es un secuencial dentro de toda la empresa. 
     */
    @Basic
    @Column(name = "TCDCNMCL")
    private Long numeroCuotaLetra;
    
    /**
     * Numero total de cuotas en las que fue financiado el documento. 
     */
    @Basic
    @Column(name = "TCDCNMTC")
    private Long numeroTotalCuotas;
    
    /**
     * Total abonado a la cuota.   
     */
    @Basic
    @Column(name = "TCDCABNO")
    private Double totalAbono;
    
    /**
     * Saldo de la cuota.
     */
    @Basic
    @Column(name = "TCDCSLDO")
    private Double saldo;
    
    // ============================================================
    // Getters y Setters
    // ============================================================
    
    public Long getCodigo() {
        return codigo;
    }
    
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }
    
    public TempFinanciacionXDocumentoCobro getTempFinanciacionXDocumentoCobro() {
        return tempFinanciacionXDocumentoCobro;
    }

    public void setTempFinanciacionXDocumentoCobro(TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro) {
        this.tempFinanciacionXDocumentoCobro = tempFinanciacionXDocumentoCobro;
    }

    public Date getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Date fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Date getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(Date fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public Long getTipo() {
        return tipo;
    }

    public void setTipo(Long tipo) {
        this.tipo = tipo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getNumeroSecuencial() {
        return numeroSecuencial;
    }

    public void setNumeroSecuencial(Long numeroSecuencial) {
        this.numeroSecuencial = numeroSecuencial;
    }

    public Long getNumeroCuotaLetra() {
        return numeroCuotaLetra;
    }

    public void setNumeroCuotaLetra(Long numeroCuotaLetra) {
        this.numeroCuotaLetra = numeroCuotaLetra;
    }

    public Long getNumeroTotalCuotas() {
        return numeroTotalCuotas;
    }

    public void setNumeroTotalCuotas(Long numeroTotalCuotas) {
        this.numeroTotalCuotas = numeroTotalCuotas;
    }

    public Double getTotalAbono() {
        return totalAbono;
    }

    public void setTotalAbono(Double totalAbono) {
        this.totalAbono = totalAbono;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }
}