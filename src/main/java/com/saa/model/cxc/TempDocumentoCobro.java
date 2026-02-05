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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.TDCC.
 * Entity TempDocumentoCobro.
 * Entidad donde se almacenan los documentos a cobrar o generados por los clientes.
 * Incluye facturas, notas de crédito, etc.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TDCC", schema = "CBR")
@SequenceGenerator(name = "SQ_TDCCCDGO", sequenceName = "CBR.SQ_TDCCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TempDocumentoCobroAll", query = "select e from TempDocumentoCobro e"),
    @NamedQuery(name = "TempDocumentoCobroId", query = "select e from TempDocumentoCobro e where e.codigo = :id")
})
public class TempDocumentoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "TDCCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TDCCCDGO")
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
    @Column(name = "TDCCFCDC")
    private LocalDateTime fechaDocumento;

    /**
     * Razon social del proveedor o persona que entrega el documento.
     */
    @Basic
    @Column(name = "TDCCRZSC")
    private String razonSocial;

    /**
     * Ruc del proveedor o persona que entrega el documento.
     */
    @Basic
    @Column(name = "TDCCRUCC")
    private String ruc;

    /**
     * Direccion del proveedor o persona que entrega el documento.
     */
    @Basic
    @Column(name = "TDCCDRCC")
    private String direccion;

    /**
     * Numero de días de vencimiento del documento.
     */
    @Basic
    @Column(name = "TDCCDSVN")
    private Long diasVencimiento;

    /**
     * Fecha de vencimiento del documento.
     */
    @Basic
    @Column(name = "TDCCFCVN")
    private LocalDateTime fechaVencimiento;

    /**
     * Numero de serie del documento.
     */
    @Basic
    @Column(name = "TDCCSREE")
    private String numeroSerie;

    /**
     * Numero de documento (string tal como aparece impreso).
     */
    @Basic
    @Column(name = "TDCCNMRO")
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
    @Column(name = "TDCCMSSS")
    private Long mes;

    /**
     * Numero de año en el que se encuentra el documento.
     */
    @Basic
    @Column(name = "TDCCANIO")
    private Long anio;

    /**
     * Numero de autorización del documento.
     */
    @Basic
    @Column(name = "TDCCNMAT")
    private Long numeroAutorizacion;

    /**
     * Fecha de autorización del documento.
     */
    @Basic
    @Column(name = "TDCCFCAT")
    private LocalDateTime fechaAutorizacion;

    /**
     * Numero de resolución del documento.
     */
    @Basic
    @Column(name = "TDCCRSLN")
    private String numeroResolucion;

    /**
     * Valor total del documento.
     */
    @Basic
    @Column(name = "TDCCTOTT")
    private Double total;

    /**
     * Valor total abonado al documento.
     */
    @Basic
    @Column(name = "TDCCABNO")
    private Double abono;

    /**
     * Valor total del saldo del documento.
     */
    @Basic
    @Column(name = "TDCCSLDO")
    private Double saldo;

    /**
     * Asiento relacionado con el documento.
     */
    @ManyToOne
    @JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Id del fisico utilizado en el documento.
     */
    @Basic
    @Column(name = "TDCCFISC")
    private Long idFisico;

    /**
     * Tipo de forma de cobro.
     */
    @Basic
    @Column(name = "TDCCTPFO")
    private Long tipoFormaCobro;

    /**
     * Numero de documento en formato numerico.
     */
    @Basic
    @Column(name = "TDCCNMNR")
    private Long numeroDocumentoNumber;

    /**
     * Rubro para estado del documento (rubro 78).
     */
    @Basic
    @Column(name = "TDCCRBE1")
    private Long rubroEstadoP;

    /**
     * Detalle de rubro para estado del documento.
     */
    @Basic
    @Column(name = "TDCCRBE2")
    private Long rubroEstadoH;

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

    public Periodo getPeriodo() {
        return periodo;
    }

    public void setPeriodo(Periodo periodo) {
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

}
