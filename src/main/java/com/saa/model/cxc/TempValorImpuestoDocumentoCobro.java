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
 * Pojo mapeo de tabla CBR.TIDC.
 * Entity TempValorImpuestoDocumentoCobro.
 * Valores de impuestos aplicados a un documento de cobro. 
 * Se llena de acuerdo a las reglas de la entidad Impuestos por Documento (pgs.ixdc).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TIDC", schema = "CBR")
@SequenceGenerator(name = "SQ_TIDCCDGO", sequenceName = "CBR.SQ_TIDCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TempValorImpuestoDocumentoCobroAll", query = "select e from TempValorImpuestoDocumentoCobro e"),
    @NamedQuery(name = "TempValorImpuestoDocumentoCobroId", query = "select e from TempValorImpuestoDocumentoCobro e where e.codigo = :id")
})
public class TempValorImpuestoDocumentoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "TIDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TIDCCDGO")
    private Long codigo;

    /**
     * Documento de cobro al que pertenecen los valores.
     */
    @ManyToOne
    @JoinColumn(name = "TDCCCDGO", referencedColumnName = "TDCCCDGO")
    private TempDocumentoCobro tempDocumentoCobro;

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
    @Column(name = "TIDCNMBR")
    private String nombre;

    /**
     * Porcentaje aplicado como impuesto.
     */
    @Basic
    @Column(name = "TIDCPRCT")
    private Double porcentaje;

    /**
     * Codigo alterno del valor del documento sobre el que se aplicó el impuesto.
     * Tomado de la entidad ResumenValoresDocumento (ValoresXDocumento).
     */
    @Basic
    @Column(name = "TIDCCAVD")
    private Long codigoAlternoValor;

    /**
     * Valor base sobre el que se aplicó el impuesto.
     */
    @Basic
    @Column(name = "TIDCVLSA")
    private Double valorBase;

    /**
     * Valor del impuesto.
     */
    @Basic
    @Column(name = "TIDCVLRR")
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
     */
    public TempDocumentoCobro getTempDocumentoCobro() {
        return tempDocumentoCobro;
    }

    /**
     * Asigna Documento de cobro al que pertenecen los valores.
     */
    public void setTempDocumentoCobro(TempDocumentoCobro tempDocumentoCobro) {
        this.tempDocumentoCobro = tempDocumentoCobro;
    }

    
    /**
     * Obtiene Detalle de impuesto aplicado.
     
    public DetalleImpuesto getDetalleImpuesto() {
        return detalleImpuesto;
    }

    /**
     * Asigna Detalle de impuesto aplicado.
    
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
     * Obtiene Codigo alterno del valor del documento sobre el que se aplicó el impuesto.
     */
    public Long getCodigoAlternoValor() {
        return codigoAlternoValor;
    }

    /**
     * Asigna Codigo alterno del valor del documento sobre el que se aplicó el impuesto.
     */
    public void setCodigoAlternoValor(Long codigoAlternoValor) {
        this.codigoAlternoValor = codigoAlternoValor;
    }

    /**
     * Obtiene Valor base sobre el que se aplicó el impuesto.
     */
    public Double getValorBase() {
        return valorBase;
    }

    /**
     * Asigna Valor base sobre el que se aplicó el impuesto.
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
