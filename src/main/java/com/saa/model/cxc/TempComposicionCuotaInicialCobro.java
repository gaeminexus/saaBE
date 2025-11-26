/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ("Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en conformidad 
 * con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
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
 *  @author GaemiSoft
 *  Pojo mapeo de tabla CBR.TCIC.
 *  Entity TempComposicionCuotaInicialCobro.
 *  Composición de la cuota inicial para cobros.
 *  Determina qué valores del documento se incluirán en la cuota inicial.
 *  Los valores se los toma de la entidad de resumen de valores por documento (TRDC).
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
    private Long codigo;

    /**
     * Resumen de valores de documento de cobro que se incluye en la cuota inicial.
     */
    private TempResumenValorDocumentoCobro tempResumenValorDocumentoCobro;

    /**
     * Valor que se incluirá en la cuota inicial.
     * Puede ser el total o una parte del total de uno de los valores del resumen por documento.
     */
    private Double valor;

    /**
     * Valor total del resumen de valores por documento.
     */
    private Double valorResumen;

    /**
     * Financiación a la que pertenece la cuota inicial.
     */
    private TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro;

    /**
     * Obtiene codigo
     */
    @Id
    @Column(name = "TCICCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCICCDGO")
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna para el codigo.
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    /**
     * Obtiene Resumen de valores del documento de cobro que se incluye en la cuota inicial.
     * @return : Resumen de valores del documento de cobro.
     */
    @ManyToOne
    @JoinColumn(name = "TRDCCDGO", referencedColumnName = "TRDCCDGO")
    public TempResumenValorDocumentoCobro getTempResumenValorDocumentoCobro() {
        return tempResumenValorDocumentoCobro;
    }

    /**
     * Asigna Resumen de valores del documento de cobro.
     * @param tempResumenValorDocumentoCobro : Resumen de valores del documento.
     */
    public void setTempResumenValorDocumentoCobro(TempResumenValorDocumentoCobro tempResumenValorDocumentoCobro) {
        this.tempResumenValorDocumentoCobro = tempResumenValorDocumentoCobro;
    }

    /**
     * Obtiene valor que se incluirá en la cuota inicial.
     */
    @Basic
    @Column(name = "TCICVLRR")
    public Double getValor() {
        return valor;
    }

    /**
     * Asigna valor que se incluirá en la cuota inicial.
     */
    public void setValor(Double valor) {
        this.valor = valor;
    }

    /**
     * Obtiene el valor total del resumen por documento.
     */
    @Basic
    @Column(name = "TCICVLRV")
    public Double getValorResumen() {
        return valorResumen;
    }

    /**
     * Asigna el valor total del resumen por documento.
     */
    public void setValorResumen(Double valorResumen) {
        this.valorResumen = valorResumen;
    }

    /**
     * Obtiene la financiación a la que pertenece la cuota inicial.
     */
    @ManyToOne
    @JoinColumn(name = "TFDCCDGO", referencedColumnName = "TFDCCDGO")
    public TempFinanciacionXDocumentoCobro getTempFinanciacionXDocumentoCobro() {
        return tempFinanciacionXDocumentoCobro;
    }

    /**
     * Asigna la financiación a la que pertenece la cuota inicial.
     */
    public void setTempFinanciacionXDocumentoCobro(TempFinanciacionXDocumentoCobro tempFinanciacionXDocumentoCobro) {
        this.tempFinanciacionXDocumentoCobro = tempFinanciacionXDocumentoCobro;
    }

}
