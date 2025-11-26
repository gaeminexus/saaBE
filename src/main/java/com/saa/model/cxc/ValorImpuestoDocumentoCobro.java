/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de Compuseg Cía. Ltda. ("Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en conformidad con los términos 
 * del acuerdo de licencia que ha introducido dentro de Compuseg
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
 * Pojo mapeo de tabla CBR.VIDC.
 * Entity ValorImpuestoDocumentoCobro.
 * Valores de impuestos que se aplicaron a un documento de cobro. 
 * Se llena de acuerdo a las reglas de la entidad Impuestos por Documento (pgs.ixdc).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "VIDC", schema = "CBR")
@SequenceGenerator(name = "SQ_VIDCCDGO", sequenceName = "CBR.SQ_VIDCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "ValorImpuestoDocumentoCobroAll", query = "select e from ValorImpuestoDocumentoCobro e"),
    @NamedQuery(name = "ValorImpuestoDocumentoCobroId", query = "select e from ValorImpuestoDocumentoCobro e where e.codigo = :id")
})
public class ValorImpuestoDocumentoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "VIDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_VIDCCDGO")
    private Long codigo;

    /**
     * Documento de cobro al que pertenecen los valores.
     */
    @ManyToOne
    @JoinColumn(name = "DCMCCDGO", referencedColumnName = "DCMCCDGO")
    private DocumentoCobro documentoCobro;

    /**
     * Detalle de impuesto aplicado.
     
    @ManyToOne
    @JoinColumn(name = "DTIMCDGO", referencedColumnName = "DTIMCDGO")
    private DetalleImpuesto detalleImpuesto;
    */

    /**
     * Nombre del impuesto aplicado.
     */
    @Basic
    @Column(name = "VIDCNMBR")
    private String nombre;

    /**
     * Porcentaje aplicado como impuesto.
     */
    @Basic
    @Column(name = "VIDCPRCT")
    private Double porcentaje;

    /**
     * Codigo alterno del valor del documento sobre el que se aplicó el impuesto. 
     * Tomado de la entidad ResumenValoresDocumento referente a la entidad ValoresXDocumento.
     */
    @Basic
    @Column(name = "VIDCCAVD")
    private Long codigoAlternoValor;

    /**
     * Valor sobre el que se aplicó el impuesto.
     */
    @Basic
    @Column(name = "VIDCVLSA")
    private Double valorBase;

    /**
     * Valor del impuesto.
     */
    @Basic
    @Column(name = "VIDCVLRR")
    private Double valor;

    // ============================================================
    // Getters y Setters
    // ============================================================

    /**
     * Obtiene codigo.
     */
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna codigo.
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    /**
     * Obtiene Documento de cobro al que pertenecen los valores.
     * @return Documento de cobro al que pertenecen los valores.
     */
    public DocumentoCobro getDocumentoCobro() {
        return documentoCobro;
    }

    /**
     * Asigna Documento de cobro al que pertenecen los valores.
     * @param documentoCobro Documento de cobro al que pertenecen los valores.
     */
    public void setDocumentoCobro(DocumentoCobro documentoCobro) {
        this.documentoCobro = documentoCobro;
    }

    /**
     * Obtiene Detalle de impuesto aplicado.
     * @return Detalle de impuesto aplicado.
     
    public DetalleImpuesto getDetalleImpuesto() {
        return detalleImpuesto;
    }
    */

    /**
     * Asigna Detalle de impuesto aplicado.
     * @param detalleImpuesto Detalle de impuesto aplicado.
     
    public void setDetalleImpuesto(DetalleImpuesto detalleImpuesto) {
        this.detalleImpuesto = detalleImpuesto;
    }
    */

    /**
     * Obtiene Nombre del impuesto aplicado.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Asigna Nombre del impuesto aplicado.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Obtiene Porcentaje aplicado como impuesto.
     */
    public Double getPorcentaje() {
        return porcentaje;
    }

    /**
     * Asigna Porcentaje aplicado como impuesto.
     */
    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }

    /**
     * Obtiene Código alterno del valor del documento sobre el que se aplicó el impuesto.
     */
    public Long getCodigoAlternoValor() {
        return codigoAlternoValor;
    }

    /**
     * Asigna Código alterno del valor del documento sobre el que se aplicó el impuesto.
     */
    public void setCodigoAlternoValor(Long codigoAlternoValor) {
        this.codigoAlternoValor = codigoAlternoValor;
    }

    /**
     * Obtiene Valor sobre el que se aplicó el impuesto.
     */
    public Double getValorBase() {
        return valorBase;
    }

    /**
     * Asigna Valor sobre el que se aplicó el impuesto.
     */
    public void setValorBase(Double valorBase) {
        this.valorBase = valorBase;
    }

    /**
     * Obtiene Valor del impuesto.
     */
    public Double getValor() {
        return valor;
    }

    /**
     * Asigna Valor del impuesto.
     */
    public void setValor(Double valor) {
        this.valor = valor;
    }

}
