/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
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
 * Pojo mapeo de tabla CBR.TPFC.
 * Entity CobrosArbitrariosXFinanciacionCobro.
 * Cobros arbitrarios de financiación de pago 
 * en caso de que no se financie en pagos periódicos sino en pagos arbitrarios.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TPFC", schema = "CBR")
@SequenceGenerator(name = "SQ_TPFCCDGO", sequenceName = "CBR.SQ_TPFCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TempPagosArbitrariosXFinanciacionCobroAll", query = "select e from TempPagosArbitrariosXFinanciacionCobro e"),
    @NamedQuery(name = "TempPagosArbitrariosXFinanciacionCobroId", query = "select e from TempPagosArbitrariosXFinanciacionCobro e where e.codigo = :id")
})
public class TempPagosArbitrariosXFinanciacionCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "TPFCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TPFCCDGO")
    private Long codigo;

    /**
     * Financiacion a la que pertenece la cuota inicial.
     */
    @ManyToOne
    @JoinColumn(name = "TFDCCDGO", referencedColumnName = "TFDCCDGO")
    private TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro;

    /**
     * Numero de dia del mes en que se debe pagar. 
     */
    @Basic
    @Column(name = "TPFCDAAA")
    private Long diaCobro;

    /**
     * Numero de mes en que se debe pagar.  
     */
    @Basic
    @Column(name = "TPFCMSSS")
    private Long mesCobro;

    /**
     * Numero de anio en que se debe pagar.  
     */
    @Basic
    @Column(name = "TPFCANOO")
    private Long anioCobro;

    /**
     * Fecha en la que se debe realizar el pago.  
     */
    @Basic
    @Column(name = "TPFCFCPG")
    private LocalDateTime fechaCobro;

    /**
     * Valor del pago.
     */
    @Basic
    @Column(name = "TPFCVLRR")
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

    public TempFinanciacionXDocumentoCobro getTempFinanciacionXDocumentoCobro() {
        return tempFinanciacionXDocumentoCobro;
    }

    public void setTempFinanciacionXDocumentoCobro(TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro) {
        this.tempFinanciacionXDocumentoCobro = tempFinanciacionXDocumentoCobro;
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
