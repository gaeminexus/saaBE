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
 * Pojo mapeo de tabla CBR.FXDC.
 * Entity FinanciacionXDocumentoCobro.
 * Financiacion por documento de cobro. Contiene la financiacion de un documento.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FXDC", schema = "CBR")
@NamedQueries({
    @NamedQuery(name = "FinanciacionXDocumentoCobroAll", query = "select e from FinanciacionXDocumentoCobro e"),
    @NamedQuery(name = "FinanciacionXDocumentoCobroId", query = "select e from FinanciacionXDocumentoCobro e where e.codigo = :id")
})
public class FinanciacionXDocumentoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "FXDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "SQ_FXDCCDGO", sequenceName = "CBR.SQ_FXDCCDGO", allocationSize = 1)
    private Long codigo;
    
    /**
     * Documento de cobro al que pertenecen los valores.
     */
    @ManyToOne
    @JoinColumn(name = "DCMCCDGO", referencedColumnName = "DCMCCDGO")
    private DocumentoCobro documentoCobro;
    
    /**
     * Tipo de financiacion. 1 = contado, 2 = credito.
     */
    @Basic
    @Column(name = "FXDCTPFN")
    private Long tipoFinanciacion;
    
    /**
     * Campo que indica si es que aplica interes. 1 = si aplica interes, 0 = no aplica interes.
     */
    @Basic
    @Column(name = "FXDCAPIN")
    private Long aplicaInteres;
    
    /**
     * Porcentaje de interes en caso de que aplique interés.
     */
    @Basic
    @Column(name = "FXDCPRIN")
    private Double porcentajeInteres;
    
    /**
     * Factor de interés. 
     * Es el número decimal por el que se multiplica para el cálculo de interes.
     */
    @Basic
    @Column(name = "FXDCFTIN")
    private Double factorInteres;
    
    /**
     * Campo que indica si es que aplica cuota inicial. 1 = si aplica cuota inicial, 0 = no aplica cuota inicial.
     */
    @Basic
    @Column(name = "FXDCAPCI")
    private Long aplicaCuotaInicial;
    
    /**
     * Valor de porcentaje de la cuota inicial. es el porcentaje que se aplica al subtotal.
     */
    @Basic
    @Column(name = "FXDCVPRC")
    private Double valorPorcentaCI;
    
    /**
     * Valor numérico de la cuota inicia. 
     * En caso de que se desee aplicar un valor especifico al que luego se le podrá sumar 
     * los impuestos en caso de que la cuota inicial sea acumulada.
     */
    @Basic
    @Column(name = "FXDCVNMC")
    private Double valorFijoCI;
    
    /**
     * Tipo de cuota inicial. 
     * 1 = acumulada(cuando es la union de un valor mas los impuestos seleccionados), 
     * 2 = fijo(cuando se aplica un valor fijo a la cuota inicial).
     */
    @Basic
    @Column(name = "FXDCTPCI")
    private Long tipoCuotaInicial;
    
    /**
     * Valor de la cuota inicial sobre la base. 
     * Es el calculo de la base por el porcentaje de cuota inicial o 
     * Es el valor numerico fijo que se encuentra en el campo fxdpvnmc
     */
    @Basic
    @Column(name = "FXDCVCIB")
    private Double valorInicialCI;
    
    /**
     * Valor total de la cuota inicial. 
     * Incluye el valor de la cuota sobre la base y 
     * los valores de impuestos seleccionados en caso de que sea de tipo acumulada.
     */
    @Basic
    @Column(name = "FXDCVTCI")
    private Double valorTotalCI;
    
    /**
     * Numero de cobros a financiarse. 
     * Sin tomar en cuenta la cuota inicial.
     */
    @Basic
    @Column(name = "FXDCNMPG")
    private Long numeroCobros;
    
    /**
     * Tipo de cobros. 1 = cuota, 2 = letras, 3 = mixto.
     */
    @Basic
    @Column(name = "FXDCTPCA")
    private Long tipoCobros;
    
    /**
     * Rubro para periodicidad de cobro. Tomado del rubro 79.
     */
    @Basic
    @Column(name = "FXDCRYYA")
    private Long rubroPeriodicidadP;
    
    /**
     * Detalle de Rubro para periodicidad de cobro. Tomado del rubro 79.
     */
    @Basic
    @Column(name = "FXDCRZZA")
    private Long rubroPeriodicidadH;
    
    /**
     * Tipo de distribucion de periodicidad de cobro. 1 = periodico fijo(La periodicidad se la tomaria del rubro 79), 
     * 2 = Arbitrario =  cobros que tienen una periodicidad fija sino arbitraria.
     */
    @Basic
    @Column(name = "FXDCTPDP")
    private Long tipoPeriodicidadCobro;
    
    /**
     * Campo que indica que esta financiacion depende de la financiacion de otro documento.
     */
    @Basic
    @Column(name = "FXDCDPOF")
    private Long dependeOtraFinanciacion;
    
    /**
     * Numero de documento del que depende. 
     * Tomado del campo numeroDocumentoString de la entidad DocumentoCobro.
     */
    @Basic
    @Column(name = "FXDCNDCD")
    private String numeroDocumentoDepende;
    
    /**
     * Id del documento del que depende.
     */
    @Basic
    @Column(name = "FXDCIDDC")
    private Long idDepende;
    
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
    
    public Long getTipoFinanciacion() {
        return tipoFinanciacion;
    }
    
    public void setTipoFinanciacion(Long tipoFinanciacion) {
        this.tipoFinanciacion = tipoFinanciacion;
    }
    
    public Long getAplicaInteres() {
        return aplicaInteres;
    }
    
    public void setAplicaInteres(Long aplicaInteres) {
        this.aplicaInteres = aplicaInteres;
    }
    
    public Double getPorcentajeInteres() {
        return porcentajeInteres;
    }
    
    public void setPorcentajeInteres(Double porcentajeInteres) {
        this.porcentajeInteres = porcentajeInteres;
    }
    
    public Double getFactorInteres() {
        return factorInteres;
    }
    
    public void setFactorInteres(Double factorInteres) {
        this.factorInteres = factorInteres;
    }
    
    public Long getAplicaCuotaInicial() {
        return aplicaCuotaInicial;
    }
    
    public void setAplicaCuotaInicial(Long aplicaCuotaInicial) {
        this.aplicaCuotaInicial = aplicaCuotaInicial;
    }
    
    public Double getValorPorcentaCI() {
        return valorPorcentaCI;
    }
    
    public void setValorPorcentaCI(Double valorPorcentaCI) {
        this.valorPorcentaCI = valorPorcentaCI;
    }
    
    public Double getValorFijoCI() {
        return valorFijoCI;
    }
    
    public void setValorFijoCI(Double valorFijoCI) {
        this.valorFijoCI = valorFijoCI;
    }
    
    public Long getTipoCuotaInicial() {
        return tipoCuotaInicial;
    }
    
    public void setTipoCuotaInicial(Long tipoCuotaInicial) {
        this.tipoCuotaInicial = tipoCuotaInicial;
    }
    
    public Double getValorInicialCI() {
        return valorInicialCI;
    }
    
    public void setValorInicialCI(Double valorInicialCI) {
        this.valorInicialCI = valorInicialCI;
    }
    
    public Double getValorTotalCI() {
        return valorTotalCI;
    }
    
    public void setValorTotalCI(Double valorTotalCI) {
        this.valorTotalCI = valorTotalCI;
    }
    
    public Long getNumeroCobros() {
        return numeroCobros;
    }
    
    public void setNumeroCobros(Long numeroCobros) {
        this.numeroCobros = numeroCobros;
    }
    
    public Long getTipoCobros() {
        return tipoCobros;
    }
    
    public void setTipoCobros(Long tipoCobros) {
        this.tipoCobros = tipoCobros;
    }
    
    public Long getRubroPeriodicidadP() {
        return rubroPeriodicidadP;
    }
    
    public void setRubroPeriodicidadP(Long rubroPeriodicidadP) {
        this.rubroPeriodicidadP = rubroPeriodicidadP;
    }
    
    public Long getRubroPeriodicidadH() {
        return rubroPeriodicidadH;
    }
    
    public void setRubroPeriodicidadH(Long rubroPeriodicidadH) {
        this.rubroPeriodicidadH = rubroPeriodicidadH;
    }
    
    public Long getTipoPeriodicidadCobro() {
        return tipoPeriodicidadCobro;
    }
    
    public void setTipoPeriodicidadCobro(Long tipoPeriodicidadCobro) {
        this.tipoPeriodicidadCobro = tipoPeriodicidadCobro;
    }
    
    public Long getDependeOtraFinanciacion() {
        return dependeOtraFinanciacion;
    }
    
    public void setDependeOtraFinanciacion(Long dependeOtraFinanciacion) {
        this.dependeOtraFinanciacion = dependeOtraFinanciacion;
    }
    
    public String getNumeroDocumentoDepende() {
        return numeroDocumentoDepende;
    }
    
    public void setNumeroDocumentoDepende(String numeroDocumentoDepende) {
        this.numeroDocumentoDepende = numeroDocumentoDepende;
    }
    
    public Long getIdDepende() {
        return idDepende;
    }
    
    public void setIdDepende(Long idDepende) {
        this.idDepende = idDepende;
    }
    
}