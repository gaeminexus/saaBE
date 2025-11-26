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
import java.util.List;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.TFDC.
 * Entity TempFinanciacionXDocumentoCobro.
 * Financiacion por documento de cobro. Contiene la financiacion de un documento.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TFDC", schema = "CBR")
@SequenceGenerator(name = "SQ_TFDCCDGO", sequenceName = "CBR.SQ_TFDCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TempFinanciacionXDocumentoCobroAll", query = "select e from TempFinanciacionXDocumentoCobro e"),
    @NamedQuery(name = "TempFinanciacionXDocumentoCobroId", query = "select e from TempFinanciacionXDocumentoCobro e where e.codigo = :id")
})
public class TempFinanciacionXDocumentoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "TFDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TFDCCDGO")
    private Long codigo;

    /**
     * Documento de cobro al que pertenecen los valores.
     */
    @ManyToOne
    @JoinColumn(name = "TDCCCDGO", referencedColumnName = "TDCCCDGO")
    private TempDocumentoCobro tempDocumentoCobro;

    /**
     * Tipo de financiacion. 1 = contado, 2 = credito.
     */
    @Basic
    @Column(name = "TFDCTPFN")
    private Long tipoFinanciacion;

    /**
     * Campo que indica si es que aplica interes. 1 = si aplica interes, 0 = no aplica interes.
     */
    @Basic
    @Column(name = "TFDCAPIN")
    private Long aplicaInteres;

    /**
     * Porcentaje de interes en caso de que aplique interés.
     */
    @Basic
    @Column(name = "TFDCPRIN")
    private Double porcentajeInteres;

    /**
     * Factor de interés.
     */
    @Basic
    @Column(name = "TFDCFTIN")
    private Double factorInteres;

    /**
     * Campo que indica si es que aplica cuota inicial. 
     */
    @Basic
    @Column(name = "TFDCAPCI")
    private Long aplicaCuotaInicial;

    /**
     * Valor de porcentaje de la cuota inicial.
     */
    @Basic
    @Column(name = "TFDCVPRC")
    private Double valorPorcentaCI;

    /**
     * Valor numérico de la cuota inicial.
     */
    @Basic
    @Column(name = "TFDCVNMC")
    private Double valorFijoCI;

    /**
     * Tipo de cuota inicial.
     */
    @Basic
    @Column(name = "TFDCTPCI")
    private Long tipoCuotaInicial;

    /**
     * Valor de la cuota inicial sobre la base.
     */
    @Basic
    @Column(name = "TFDCVCIB")
    private Double valorInicialCI;

    /**
     * Valor total de la cuota inicial.
     */
    @Basic
    @Column(name = "TFDCVTCL")
    private Double valorTotalCI;

    /**
     * Numero de cobros a financiarse.
     */
    @Basic
    @Column(name = "TFDCNMCO")
    private Long numeroCobros;

    /**
     * Tipo de cobros. 
     */
    @Basic
    @Column(name = "TFDCTPCB")
    private Long tipoCobros;

    /**
     * Rubro para periodicidad de cobro.
     */
    @Basic
    @Column(name = "TFDCRBRP")
    private Long rubroPeriodicidadP;

    /**
     * Detalle de Rubro para periodicidad de cobro.
     */
    @Basic
    @Column(name = "TFDCRBRH")
    private Long rubroPeriodicidadH;

    /**
     * Tipo de distribucion de periodicidad de cobro.
     */
    @Basic
    @Column(name = "TFDCTPPD")
    private Long tipoPeriodicidadCobro;

    /**
     * Campo que indica que esta financiacion depende de otra financiacion.
     */
    @Basic
    @Column(name = "TFDCDPOF")
    private Long dependeOtraFinanciacion;

    /**
     * Numero de documento del que depende.
     */
    @Basic
    @Column(name = "TFDCNMDP")
    private String numeroDocumentoDepende;

    /**
     * Id del documento del que depende.
     */
    @Basic
    @Column(name = "TFDCDPFD")
    private Long idDepende;

    /**
     * Listado que contiene las cuotas de la financiacion.
     */
    @OneToMany(mappedBy = "tempFinanciacionXDocumentoCobro")
    private List<TempCuotaXFinanciacionCobro> tempCuotaXFinanciacionCobros;

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
