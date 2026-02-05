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
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author GaemiSoft
 * Pojo mapeo de tabla CBR.DTDC.
 * Entity DetalleDocumentoCobro.
 * Detalle de documento en el que se almancenan todos los productos incluidos en el documento.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTDC", schema = "CBR")
@NamedQueries({
    @NamedQuery(name = "DetalleDocumentoCobroAll", query = "select e from DetalleDocumentoCobro e"),
    @NamedQuery(name = "DetalleDocumentoCobroId", query = "select e from DetalleDocumentoCobro e where e.codigo = :id")
})
public class DetalleDocumentoCobro implements Serializable {

    /**
     * Codigo de la entidad.
     */
    @Id
    @Basic
    @Column(name = "DTDCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;
    
    /**
     * Empresa a la que pertenece el detalle de documento.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;
    
    /**
     * Documento al que pertenece el detalle.
     */
    @ManyToOne
    @JoinColumn(name = "DCMCCDGO", referencedColumnName = "DCMCCDGO")
    private DocumentoCobro documentoCobro;
    
    /**
     * Producto incluido en el detalle tomado de la tabla de productos.
     */
    @ManyToOne
    @JoinColumn(name = "PRDCCDGO", referencedColumnName = "PRDCCDGO")
    private ProductoCobro productoCobro;
    
    /**
     * Descripcion del detalle.
     */
    @Basic
    @Column(name = "DTDCDSCR")
    private String descripcion;
    
    /**
     * Cantidad de productos incluidos en el detalle.
     */
    @Basic
    @Column(name = "DTDCCNTD")
    private Double cantidad;
    
    /**
     * Precio unitario del producto.   
     */
    @Basic
    @Column(name = "DTDCPRUN")
    private Double precioUnitario;
    
    /**
     * Subtotal = cantidad por precio unitario.   
     */
    @Basic
    @Column(name = "DTDCSBTT")
    private Double subtotal;
    
    /**
     * Total de impuestos que se aplican al producto. 
     */
    @Basic
    @Column(name = "DTDCTTIM")
    private Double totalImpuesto;
    
    /**
     * Total = subtota mas impuestos. 
     */
    @Basic
    @Column(name = "DTDCTTLL")
    private Double total;
    
    /**
     * Centro de costo que se aplica en caso de que maneje centro de costo. 
     */
    @ManyToOne
    @JoinColumn(name = "CNCSCDGO", referencedColumnName = "CNCSCDGO")
    private CentroCosto centroCosto;
    
    /**
     * Numero de linea de este detalle dentro del documento. 
     */
    @Basic
    @Column(name = "DTDCNMLN")
    private Long numeroLinea;
    
    /**
     * Estado del detalle de documento. 1 = activo, 2 = anulado
     */
    @Basic
    @Column(name = "DTDCESTD")
    private Long estado;
    
    /**
     * Fecha de ingreso.
     */
    @Basic
    @Column(name = "DTDCFCIN")
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

    public DocumentoCobro getDocumentoCobro() {
        return documentoCobro;
    }

    public void setDocumentoCobro(DocumentoCobro documentoCobro) {
        this.documentoCobro = documentoCobro;
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