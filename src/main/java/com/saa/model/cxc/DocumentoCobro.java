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
import java.time.LocalDateTime;
import java.util.List;

import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.Periodo;
import com.saa.model.scp.Empresa;

import com.saa.model.tsr.Titular;

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
import jakarta.persistence.Table;

/**
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.DCMC.
 * Entity DocumentoCobro.
 * Entidad donde se almancenan los documentos a cobrar o generados por los clientes. Incluye facturas, nc, etc.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DCMC", schema = "CBR")
@NamedQueries({
    @NamedQuery(name = "DocumentoCobroAll", query = "select e from DocumentoCobro e"),
    @NamedQuery(name = "DocumentoCobroId", query = "select e from DocumentoCobro e where e.codigo = :id")
})
public class DocumentoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "DCMCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;
    
    /**
     * Empresa donde se genera el documento de cobro.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;
    
    /**
     * Persona o proveedor de la que se recibe el documento.
     */
    @ManyToOne
    @JoinColumn(name = "TTLRCDGO", referencedColumnName = "TTLRCDGO")
    private Titular titular;
    
    /**
     * Tipo de documento tomado de la entidad de Tipo documento.
    @ManyToOne
    @JoinColumn(name = "STDCCDGO", referencedColumnName = "STDCCDGO")
    private SRITipoDocumento sRITipoDocumento;
    */
    
    
    /**
     * Fecha del documento.
     */
    @Basic
    @Column(name = "DCMCFCDC")
    private LocalDateTime fechaDocumento;
    
    /**
     * Razon social del proveedor o persona que entrega el documento.
     */
    @Basic
    @Column(name = "DCMCRZSC")
    private String razonSocial;
    
    /**
     * Ruc del proveedor o persona que entrega el documento.
     */
    @Basic
    @Column(name = "DCMCRUCC")
    private String ruc;
    
    /**
     * Direccion del proveedor o persona que entrega el documento.
     */
    @Basic
    @Column(name = "DCMCDRCC")
    private String direccion;
    
    /**
     * Numero de días de vencimiento del documento. Tomados desde la fecha del documento. 
     */
    @Basic
    @Column(name = "DCMCDSVN")
    private Long diasVencimiento;
    
    /**
     * Fecha de vencimiento del documento. 
     */
    @Basic
    @Column(name = "DCMCFCVN")
    private LocalDateTime fechaVencimiento;
    
    /**
     * Numero de serie del documento.
     */
    @Basic
    @Column(name = "DCMCSREE")
    private String numeroSerie;
    
    /**
     * Numero de documento. Contiene el numero de documento tal como se imprime en el fisico.
     */
    @Basic
    @Column(name = "DCMCNMRO")
    private String numeroDocumentoString;
    
    /**
     * Periodo al que pertenece el documento.
     */
    @ManyToOne
    @JoinColumn(name = "PRDOCDGO", referencedColumnName = "PRDOCDGO")
    private Periodo periodo;
    
    /**
     * Numero de mes en el que se encuentra el documento.
     */
    @Basic
    @Column(name = "DCMCMSSS")
    private Long mes;
    
    /**
     * Numero de año en el que se encuentra el documento.
     */
    @Basic
    @Column(name = "DCMCANOO")
    private Long anio;
    
    /**
     * Numero de autorizacion del documento.
     */
    @Basic
    @Column(name = "DCMCNMAU")
    private Long numeroAutorizacion;
    
    /**
     * Fecha de autorizacion del documento.
     */
    @Basic
    @Column(name = "DCMCFCAU")
    private LocalDateTime fechaAutorizacion;
    
    /**
     * Numero de resolucion del documento.
     */
    @Basic
    @Column(name = "DCMCNMRS")
    private String numeroResolucion;
    
    /**
     * Valor total del documento.
     */
    @Basic
    @Column(name = "DCMCTTLL")
    private Double total;
    
    /**
     * Valor total abonado al documento.
     */
    @Basic
    @Column(name = "DCMCABNN")
    private Double abono;
    
    /**
     * Valor total del saldo del documento.
     */
    @Basic
    @Column(name = "DCMCSLDD")
    private Double saldo;
    
    /**
     * Asiento relacionado con el documento.
     */
    @ManyToOne
    @JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;
    
    /**
     * Id del fisico utilizado en el documento. Es solo una referencia de la entidad Documento por bloque.
     */
    @Basic
    @Column(name = "DCMCIDFS")
    private Long idFisico;
    
    /**
     * Tipo de forma de cobro. 1 = Debito bancario, 2 = Tarjeta de credito, 3 = Otros.
     */
    @Basic
    @Column(name = "DCMCFRPG")
    private Long tipoFormaCobro;
    
    /**
     * Numero de documento. Contiene el valor numerico del numero de documento.
     */
    @Basic
    @Column(name = "DCMCNMRN")
    private Long numeroDocumentoNumber;
    
    /**
     * Rubro para estado de documento de cobro. Tomado del rubro 78.
     */
    @Basic
    @Column(name = "DCMCRYYA")
    private Long rubroEstadoP;
    
    /**
     * Detalle de Rubro para estado de documento de cobro. Tomado del rubro 78.
     */
    @Basic
    @Column(name = "DCMCRZZA")
    private Long rubroEstadoH;
    
    /**
     * Listado de detalle de documentos de cobro que tiene el documento.
     */
    @OneToMany(mappedBy = "documentoCobro")
    private List<DetalleDocumentoCobro> detalleDocumentoCobros = null;
    
    /**
     * Listado de los valores de impuesto que se aplicaron al documento.
     */
    @OneToMany(mappedBy = "documentoCobro")
    private List<ValorImpuestoDocumentoCobro> valorImpuestoDocumentoCobros = null;
    
    /**
     * Listado del resumen de valores del documento.
     */
    @OneToMany(mappedBy = "documentoCobro")
    private List<ResumenValorDocumentoCobro> resumenValorDocumentoCobros = null;
    
    /**
     * Listado de financiaciones relacionadas con el documento.
     */
    @OneToMany(mappedBy = "documentoCobro")
    private List<FinanciacionXDocumentoCobro> financiacionXDocumentoCobros = null;
    
    // ============================================================
    // Getters y Setters
    // ============================================================
    
    public Long getCodigo() {
        return codigo;
    }
    
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Titular getTitular() {
        return titular;
    }

    public void setPersona(Titular titular) {
        this.titular = titular;
    }
    
    /*
    public SRITipoDocumento getsRITipoDocumento() {
        return sRITipoDocumento;
    }

    public void setsRITipoDocumento(SRITipoDocumento sRITipoDocumento) {
        this.sRITipoDocumento = sRITipoDocumento;
    }
    */

    public LocalDateTime getFechaDocumento() {
        return fechaDocumento;
    }

    public void setFechaDocumento(LocalDateTime fechaDocumento) {
        this.fechaDocumento = fechaDocumento;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Long getDiasVencimiento() {
        return diasVencimiento;
    }

    public void setDiasVencimiento(Long diasVencimiento) {
        this.diasVencimiento = diasVencimiento;
    }

    public LocalDateTime getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDateTime fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public String getNumeroDocumentoString() {
        return numeroDocumentoString;
    }

    public void setNumeroDocumentoString(String numeroDocumentoString) {
        this.numeroDocumentoString = numeroDocumentoString;
    }

    public Periodo getPerido() {
        return periodo;
    }

    public void setPerido(Periodo periodo) {
        this.periodo = periodo;
    }

    public Long getMes() {
        return mes;
    }

    public void setMes(Long mes) {
        this.mes = mes;
    }

    public Long getAnio() {
        return anio;
    }

    public void setAnio(Long anio) {
        this.anio = anio;
    }

    public Long getNumeroAutorizacion() {
        return numeroAutorizacion;
    }

    public void setNumeroAutorizacion(Long numeroAutorizacion) {
        this.numeroAutorizacion = numeroAutorizacion;
    }

    public LocalDateTime getFechaAutorizacion() {
        return fechaAutorizacion;
    }

    public void setFechaAutorizacion(LocalDateTime fechaAutorizacion) {
        this.fechaAutorizacion = fechaAutorizacion;
    }

    public String getNumeroResolucion() {
        return numeroResolucion;
    }

    public void setNumeroResolucion(String numeroResolucion) {
        this.numeroResolucion = numeroResolucion;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getAbono() {
        return abono;
    }

    public void setAbono(Double abono) {
        this.abono = abono;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public Asiento getAsiento() {
        return asiento;
    }

    public void setAsiento(Asiento asiento) {
        this.asiento = asiento;
    }

    public Long getIdFisico() {
        return idFisico;
    }

    public void setIdFisico(Long idFisico) {
        this.idFisico = idFisico;
    }

    public Long getTipoFormaCobro() {
        return tipoFormaCobro;
    }

    public void setTipoFormaCobro(Long tipoFormaCobro) {
        this.tipoFormaCobro = tipoFormaCobro;
    }

    public Long getNumeroDocumentoNumber() {
        return numeroDocumentoNumber;
    }

    public void setNumeroDocumentoNumber(Long numeroDocumentoNumber) {
        this.numeroDocumentoNumber = numeroDocumentoNumber;
    }

    public Long getRubroEstadoP() {
        return rubroEstadoP;
    }

    public void setRubroEstadoP(Long rubroEstadoP) {
        this.rubroEstadoP = rubroEstadoP;
    }

    public Long getRubroEstadoH() {
        return rubroEstadoH;
    }

    public void setRubroEstadoH(Long rubroEstadoH) {
        this.rubroEstadoH = rubroEstadoH;
    }

    public List<DetalleDocumentoCobro> getDetalleDocumentoCobros() {
        return detalleDocumentoCobros;
    }

    public void setDetalleDocumentoCobros(List<DetalleDocumentoCobro> detalleDocumentoCobros) {
        this.detalleDocumentoCobros = detalleDocumentoCobros;
    }

    public List<ValorImpuestoDocumentoCobro> getValorImpuestoDocumentoCobros() {
        return valorImpuestoDocumentoCobros;
    }

    public void setValorImpuestoDocumentoCobros(List<ValorImpuestoDocumentoCobro> valorImpuestoDocumentoCobros) {
        this.valorImpuestoDocumentoCobros = valorImpuestoDocumentoCobros;
    }

    public List<ResumenValorDocumentoCobro> getResumenValorDocumentoCobros() {
        return resumenValorDocumentoCobros;
    }

    public void setResumenValorDocumentoCobros(List<ResumenValorDocumentoCobro> resumenValorDocumentoCobros) {
        this.resumenValorDocumentoCobros = resumenValorDocumentoCobros;
    }

    public List<FinanciacionXDocumentoCobro> getFinanciacionXDocumentoCobros() {
        return financiacionXDocumentoCobros;
    }

    public void setFinanciacionXDocumentoCobros(List<FinanciacionXDocumentoCobro> financiacionXDocumentoCobros) {
        this.financiacionXDocumentoCobros = financiacionXDocumentoCobros;
    }
}