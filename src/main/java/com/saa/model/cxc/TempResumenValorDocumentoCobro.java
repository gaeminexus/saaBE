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
 * Pojo mapeo de tabla CBR.TRDC.
 * Entity TempResumenValorDocumentoCobro.
 * Resumen de valores de documento de cobro.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TRDC", schema = "CBR")
@SequenceGenerator(name = "SQ_TRDCCDGO", sequenceName = "CBR.SQ_TRDCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TempResumenValorDocumentoCobroAll", query = "select e from TempResumenValorDocumentoCobro e"),
    @NamedQuery(name = "TempResumenValorDocumentoCobroId", query = "select e from TempResumenValorDocumentoCobro e where e.codigo = :id")
})
public class TempResumenValorDocumentoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "TRDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Documento de cobro al que pertenecen los valores.
     */
    @ManyToOne
    @JoinColumn(name = "TDCCCDGO", referencedColumnName = "TDCCCDGO")
    private TempDocumentoCobro tempDocumentoCobro;

    /**
     * Codigo alterno del tipo de valor aplicado. 
     * Tomado de la entidad ValoresXDocumento(pgs.vxdc).
     */
    @Basic
    @Column(name = "TRDCCATP")
    private Long codigoAlternoTipoValor;

    /**
     * Valor del impuesto.   
     */
    @Basic
    @Column(name = "TRDCVLRR")
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

   
    public TempDocumentoCobro getTempDocumentoCobro() {
        return tempDocumentoCobro;
    }

 
    public void setTempDocumentoCobro(TempDocumentoCobro tempDocumentoCobro) {
        this.tempDocumentoCobro = tempDocumentoCobro;
    }

   
    public Long getCodigoAlternoTipoValor() {
        return codigoAlternoTipoValor;
    }

 
    public void setCodigoAlternoTipoValor(Long codigoAlternoTipoValor) {
        this.codigoAlternoTipoValor = codigoAlternoTipoValor;
    }

   
    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

}
