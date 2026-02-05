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
import java.util.Date;

import com.saa.model.cnt.CentroCosto;
import com.saa.model.scp.Empresa;

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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.TDTC.
 * Entity TempDetalleDocumentoCobro.
 * Detalle del documento en el que se almacenan los productos asociados.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TDTC", schema = "CBR")
@SequenceGenerator(name = "SQ_TDTCCDGO", sequenceName = "CBR.SQ_TDTCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TempDetalleDocumentoCobroAll", query = "select e from TempDetalleDocumentoCobro e"),
    @NamedQuery(name = "TempDetalleDocumentoCobroId", query = "select e from TempDetalleDocumentoCobro e where e.codigo = :id")
})
public class TempDetalleDocumentoCobro implements Serializable {

    /**
     * Código de la entidad.
     */
    @Id
    @Basic
    @Column(name = "TDTCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TDTCCDGO")
    private Long codigo;

    /**
     * Empresa a la que pertenece el detalle del documento.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Documento al que pertenece el detalle.
     */
    @ManyToOne
    @JoinColumn(name = "TDCCCDGO", referencedColumnName = "TDCCCDGO")
    private TempDocumentoCobro tempDocumentoCobro;

    /**
     * Producto incluido en el detalle.
     */
    @ManyToOne
    @JoinColumn(name = "PRDCCDGO", referencedColumnName = "PRDCCDGO")
    private ProductoCobro productoCobro;

    /**
     * Descripción del detalle.
     */
    @Basic
    @Column(name = "TDTCDSCR")
    private String descripcion;

    /**
     * Cantidad del producto incluido.
     */
    @Basic
    @Column(name = "TDTCCNTD")
    private Double cantidad;

    /**
     * Precio unitario del producto.
     */
    @Basic
    @Column(name = "TDTCPRUN")
    private Double precioUnitario;

    /**
     * Subtotal = cantidad * precio unitario.
     */
    @Basic
    @Column(name = "TDTCSBTT")
    private Double subtotal;

    /**
     * Total de impuestos aplicados.
     */
    @Basic
    @Column(name = "TDTCTTIM")
    private Double totalImpuesto;

    /**
     * Total = subtotal + impuestos.
     */
    @Basic
    @Column(name = "TDTCTTLL")
    private Double total;

    /**
     * Centro de costo aplicado.
     */
    @ManyToOne
    @JoinColumn(name = "CNCSCDGO", referencedColumnName = "CNCSCDGO")
    private CentroCosto centroCosto;

    /**
     * Número de línea dentro del documento.
     */
    @Basic
    @Column(name = "TDTCNMLN")
    private Long numeroLinea;

    /**
     * Estado del detalle. 1 = activo, 2 = anulado.
     */
    @Basic
    @Column(name = "TDTCESTD")
    private Long estado;

    /**
     * Fecha de ingreso del detalle.
     */
    @Basic
    @Column(name = "TDTCFCIN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaIngreso;


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

    public TempDocumentoCobro getTempDocumentoCobro() {
        return tempDocumentoCobro;
    }

    public void setTempDocumentoCobro(TempDocumentoCobro tempDocumentoCobro) {
        this.tempDocumentoCobro = tempDocumentoCobro;
    }

    public ProductoCobro getProductoCobro() {
        return productoCobro;
    }

    public void setProductoCobro(ProductoCobro productoCobro) {
        this.productoCobro = productoCobro;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getCantidad() {
        return cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getTotalImpuesto() {
        return totalImpuesto;
    }

    public void setTotalImpuesto(Double totalImpuesto) {
        this.totalImpuesto = totalImpuesto;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public CentroCosto getCentroCosto() {
        return centroCosto;
    }

    public void setCentroCosto(CentroCosto centroCosto) {
        this.centroCosto = centroCosto;
    }

    public Long getNumeroLinea() {
        return numeroLinea;
    }

    public void setNumeroLinea(Long numeroLinea) {
        this.numeroLinea = numeroLinea;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Date getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(Date fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

}
