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
 * Pojo mapeo de tabla CBR.TCIC.
 * Entity TempComposicionCuotaInicialCobro.
 * Composicion de la cuota inicial para cobros. 
 * Determina qué valores del documento se incluiran en la cuota inicia. 
 * Los valores se los toma de la entidad de resumen de valores por documento (TRDC).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TCIC", schema = "CBR")
@SequenceGenerator(name = "SQ_TCICCDGO", sequenceName = "CBR.SQ_TCICCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TempComposicionCuotaInicialCobroAll", query = "select e from TempComposicionCuotaInicialCobro e"),
    @NamedQuery(name = "TempComposicionCuotaInicialCobroId", query = "select e from TempComposicionCuotaInicialCobro e where e.codigo = :id")
})
public class TempComposicionCuotaInicialCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "TCICCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCICCDGO")
    private Long codigo;
    
    /**
     * Resumen de valores de documento de cobro que se incluye en la cuota inicial.  
     */
    @ManyToOne
    @JoinColumn(name = "TRDCCDGO", referencedColumnName = "TRDCCDGO")
    private TempResumenValorDocumentoCobro tempResumenValorDocumentoCobro;
    
    /**
     * Valor de que se incluirá en la cuota inicial. 
     * Puede ser el total o una parte del total de uno de los valores del resumen de valores del documento.    
     */
    @Basic
    @Column(name = "TCICVLRR")
    private Double valor;
    
    /**
     * Valor total del resumen de valores por documento.   
     */
    @Basic
    @Column(name = "TCICVLRV")
    private Double valorResumen;    
    
    /**
     * Financiacion a la que pertenece la cuota inicial.
     */
    @ManyToOne
    @JoinColumn(name = "TFDCCDGO", referencedColumnName = "TFDCCDGO")
    private TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro;
    
    // ============================================================
    // Getters y Setters
    // ============================================================
    
    public Long getCodigo() {
        return codigo;
    }
    
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }
    
    public TempResumenValorDocumentoCobro getTempResumenValorDocumentoCobro() {
        return tempResumenValorDocumentoCobro;
    }

    public void setTempResumenValorDocumentoCobro(TempResumenValorDocumentoCobro tempResumenValorDocumentoCobro) {
        this.tempResumenValorDocumentoCobro = tempResumenValorDocumentoCobro;
    }
    
    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getValorResumen() {
        return valorResumen;
    }

    public void setValorResumen(Double valorResumen) {
        this.valorResumen = valorResumen;
    }

    public TempFinanciacionXDocumentoCobro getTempFinanciacionXDocumentoCobro() {
        return tempFinanciacionXDocumentoCobro;
    }

    public void setTempFinanciacionXDocumentoCobro(TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro) {
        this.tempFinanciacionXDocumentoCobro = tempFinanciacionXDocumentoCobro;
    }
}