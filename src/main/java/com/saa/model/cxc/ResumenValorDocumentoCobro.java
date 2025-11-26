/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 * Este software es la información confidencial y patentada de Compuseg Cía. Ltda. ("Información Confidencial").
 * Usted no puede divulgar dicha información confidencial y se utilizará solo en conformidad con los términos del
 * acuerdo de licencia que ha introducido dentro de Compuseg.
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
 * POJO mapeo de tabla CBR.RVDC.
 * Entity ResumenValorDocumentoCobro.
 * Resumen de valores de documento de cobro.
 * 
 * @author GaemiSoft
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RVDC", schema = "CBR")
@SequenceGenerator(
        name = "SQ_RVDCCDGO",
        sequenceName = "CBR.SQ_RVDCCDGO",
        allocationSize = 1
)
@NamedQueries({
        @NamedQuery(name = "ResumenValorDocumentoCobroAll",
                query = "select e from ResumenValorDocumentoCobro e"),
        @NamedQuery(name = "ResumenValorDocumentoCobroId",
                query = "select e from ResumenValorDocumentoCobro e where e.codigo = :id")
})
public class ResumenValorDocumentoCobro implements Serializable {

    /**
     * Código de la entidad.
     */
    private Long codigo;

    /**
     * Documento de cobro al que pertenecen los valores.
     */
    private DocumentoCobro documentoCobro;

    /**
     * Código alterno del tipo de valor aplicado.
     * Tomado de la entidad ValoresXDocumento (pgs.vxdc).
     */
    private Long codigoAlternoTipoValor;

    /**
     * Valor del impuesto.
     */
    private Double valor;

    /**
     * Obtiene el código.
     */
    @Id
    @Column(name = "RVDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_RVDCCDGO")
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna el código.
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    /**
     * Obtiene documento de cobro al que pertenecen los valores.
     * 
     * @return Documento de cobro.
     */
    @ManyToOne
    @JoinColumn(name = "DCMCCDGO", referencedColumnName = "DCMCCDGO")
    public DocumentoCobro getDocumentoCobro() {
        return documentoCobro;
    }

    /**
     * Asigna documento de cobro al que pertenecen los valores.
     * 
     * @param documentoCobro Documento de cobro.
     */
    public void setDocumentoCobro(DocumentoCobro documentoCobro) {
        this.documentoCobro = documentoCobro;
    }

    /**
     * Obtiene código alterno del tipo de valor aplicado.
     * 
     * @return Código alterno del tipo de valor aplicado.
     */
    @Basic
    @Column(name = "RVDCCATP")
    public Long getCodigoAlternoTipoValor() {
        return codigoAlternoTipoValor;
    }

    /**
     * Asigna código alterno del tipo de valor aplicado.
     * 
     * @param codigoAlternoTipoValor Código alterno del tipo de valor aplicado.
     */
    public void setCodigoAlternoTipoValor(Long codigoAlternoTipoValor) {
        this.codigoAlternoTipoValor = codigoAlternoTipoValor;
    }

    /**
     * Obtiene valor del impuesto.
     * 
     * @return Valor del impuesto.
     */
    @Basic
    @Column(name = "RVDCVLRR")
    public Double getValor() {
        return valor;
    }

    /**
     * Asigna valor del impuesto.
     * 
     * @param valor Valor del impuesto.
     */
    public void setValor(Double valor) {
        this.valor = valor;
    }
}
