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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.PAFC.
 * Entity PagosArbitrariosXFinanciacionCobro.
 * Cobros arbitrarios de financiacion de pago 
 * en caso de que no se financie en pagos periodicos sino en pagos arbitrarios.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PAFC", schema = "CBR")
@NamedQueries({
    @NamedQuery(name = "PagosArbitrariosXFinanciacionCobroAll", query = "select e from PagosArbitrariosXFinanciacionCobro e"),
    @NamedQuery(name = "PagosArbitrariosXFinanciacionCobroId", query = "select e from PagosArbitrariosXFinanciacionCobro e where e.codigo = :id")
})
public class PagosArbitrariosXFinanciacionCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "PAFCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "SQ_PAFCCDGO", sequenceName = "CBR.SQ_PAFCCDGO", allocationSize = 1)
    private Long codigo;
    
    /**
     * Financiacion a la que pertenece la cuota inicial.
     */
    @ManyToOne
    @JoinColumn(name = "FXDCCDGO", referencedColumnName = "FXDCCDGO")
    private FinanciacionXDocumentoCobro financiacionXDocumentoCobro;
    
    /**
     * Numero de dia del mes en que se debe pagar.
     */
    @Basic
    @Column(name = "PAFCDAAA")
    private Long diaCobro;
    
    /**
     * Numero de mes en que se debe pagar.
     */
    @Basic
    @Column(name = "PAFCMSSS")
    private Long mesCobro;
    
    /**
     * Numero de anio en que se debe pagar.
     */
    @Basic
    @Column(name = "PAFCANOO")
    private Long anioCobro;
    
    /**
     * Fecha en la que se debe realizar el pago.
     */
    @Basic
    @Column(name = "PAFCFCPG")
    private LocalDateTime fechaCobro;
    
    /**
     * Valor del pago.
     */
    @Basic
    @Column(name = "PAFCVLRR")
    private Double valor;
    
    // ============================================================
    // Getters y Setters
    // ============================================================
    
    public Long getCodigo() {
        return codigo;
    }
    
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }
    
    public FinanciacionXDocumentoCobro getFinanciacionXDocumentoCobro() {
        return financiacionXDocumentoCobro;
    }
    
    public void setFinanciacionXDocumentoCobro(FinanciacionXDocumentoCobro financiacionXDocumentoCobro) {
        this.financiacionXDocumentoCobro = financiacionXDocumentoCobro;
    }
    
    public Long getDiaCobro() {
        return diaCobro;
    }
    
    public void setDiaCobro(Long diaCobro) {
        this.diaCobro = diaCobro;
    }
    
    public Long getMesCobro() {
        return mesCobro;
    }
    
    public void setMesCobro(Long mesCobro) {
        this.mesCobro = mesCobro;
    }
    
    public Long getAnioCobro() {
        return anioCobro;
    }
    
    public void setAnioCobro(Long anioCobro) {
        this.anioCobro = anioCobro;
    }
    
    public LocalDateTime getFechaCobro() {
        return fechaCobro;
    }
    
    public void setFechaCobro(LocalDateTime fechaCobro) {
        this.fechaCobro = fechaCobro;
    }
    
    public Double getValor() {
        return valor;
    }
    
    public void setValor(Double valor) {
        this.valor = valor;
    }
}