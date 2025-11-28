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
import jakarta.persistence.Table;

/**
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.RVDC.
 * Entity ResumenValorDocumentoCobro.
 * Resumen de valores de documento de cobro.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RVDC", schema = "CBR")
@NamedQueries({
    @NamedQuery(name = "ResumenValorDocumentoCobroAll", query = "select e from ResumenValorDocumentoCobro e"),
    @NamedQuery(name = "ResumenValorDocumentoCobroId", query = "select e from ResumenValorDocumentoCobro e where e.codigo = :id")
})
public class ResumenValorDocumentoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "RVDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;
    
    /**
     * Documento de cobro al que pertenecen los valores.
     */
    @ManyToOne
    @JoinColumn(name = "DCMCCDGO", referencedColumnName = "DCMCCDGO")
    private DocumentoCobro documentoCobro;
    
    /**
     * Codigo alterno del tipo de valor aplicado. 
     * Tomado de la entidad ValoresXDocumento(pgs.vxdc).
     */
    @Basic
    @Column(name = "RVDCCATP")
    private Long codigoAlternoTipoValor;
    
    /**
     * Valor del impuesto.   
     */
    @Basic
    @Column(name = "RVDCVLRR")
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
    
    public DocumentoCobro getDocumentoCobro() {
        return documentoCobro;
    }
    
    public void setDocumentoCobro(DocumentoCobro documentoCobro) {
        this.documentoCobro = documentoCobro;
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