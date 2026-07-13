/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
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
import jakarta.persistence.Table;

/**
 * Entity DetalleFactura.
 * Almacena el detalle de items de la factura electrónica.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTFC", schema = "CBR")
@NamedQueries({
	@NamedQuery(name = "DetalleFacturaAll", query = "select e from DetalleFactura e"),
	@NamedQuery(name = "DetalleFacturaId", query = "select e from DetalleFactura e where e.id = :id")
})
public class DetalleFactura implements Serializable {

	/**
	 * Id de Tabla.
	 */
	@Basic
	@Id
	@Column(name = "ID", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Factura a la que pertenece el detalle.
	 */
	@ManyToOne
	@JoinColumn(name = "FACTURA", referencedColumnName = "ID")
	private Factura factura;
	
	/**
	 * Descripción del producto o servicio.
	 */
	@Basic
	@Column(name = "DESCRIPCION", length = 500)
	private String descripcion;
	
	/**
	 * Cantidad del producto.
	 */
	@Basic
	@Column(name = "CANTIDAD")
	private Double cantidad;
	
	/**
	 * Valor unitario del producto.
	 */
	@Basic
	@Column(name = "VALOR")
	private Double valor;
	
	/**
	 * Subtotal (cantidad * valor).
	 */
	@Basic
	@Column(name = "SUBTOTAL")
	private Double subTotal;
	
	/**
	 * Descuento aplicado al item.
	 */
	@Basic
	@Column(name = "DESCUENTO")
	private Double descuento;
	
	/**
	 * Base imponible (subtotal - descuento).
	 */
	@Basic
	@Column(name = "BASEIMPONIBLE")
	private Double baseImponible;
	
	/**
	 * Porcentaje de IVA aplicado.
	 */
	@Basic
	@Column(name = "PORCENTAJEIVA")
	private Long porcentajeIVA;
	
	/**
	 * Valor del IVA.
	 */
	@Basic
	@Column(name = "VALORIVA")
	private Double valorIVA;
	
	/**
	 * Porcentaje de ICE aplicado.
	 */
	@Basic
	@Column(name = "PORCENTAJEICE")
	private Long porcentajeICE;
	
	/**
	 * Valor del ICE.
	 */
	@Basic
	@Column(name = "VALORICE")
	private Double valorICE;
	
	/**
	 * Subsidio aplicado.
	 */
	@Basic
	@Column(name = "SUBSIDIO")
	private Double subsidio;
	
	/**
	 * Precio sin subsidio.
	 */
	@Basic
	@Column(name = "PRECIOSINSUB")
	private Double precioSinSub;
	
	/**
	 * Total del item.
	 */
	@Basic
	@Column(name = "TOTAL")
	private Double total;
	
	/**
	 * Producto relacionado.
	 */
	@ManyToOne
	@JoinColumn(name = "PRODUCTO", referencedColumnName = "ID")
	private ProductoCobro producto;
	
	/**
	 * Código de IVA del SRI.
	 */
	@Basic
	@Column(name = "CODIGOIVASRI")
	private Long codigoIVASRI;
	
	/**
	 * Estado del registro.
	 */
	@Basic
	@Column(name = "ESTADO")
	private Long estado;

	// Getters y Setters
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Factura getFactura() {
		return factura;
	}

	public void setFactura(Factura factura) {
		this.factura = factura;
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

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public Double getSubTotal() {
		return subTotal;
	}

	public void setSubTotal(Double subTotal) {
		this.subTotal = subTotal;
	}

	public Double getDescuento() {
		return descuento;
	}

	public void setDescuento(Double descuento) {
		this.descuento = descuento;
	}

	public Double getBaseImponible() {
		return baseImponible;
	}

	public void setBaseImponible(Double baseImponible) {
		this.baseImponible = baseImponible;
	}

	public Long getPorcentajeIVA() {
		return porcentajeIVA;
	}

	public void setPorcentajeIVA(Long porcentajeIVA) {
		this.porcentajeIVA = porcentajeIVA;
	}

	public Double getValorIVA() {
		return valorIVA;
	}

	public void setValorIVA(Double valorIVA) {
		this.valorIVA = valorIVA;
	}

	public Long getPorcentajeICE() {
		return porcentajeICE;
	}

	public void setPorcentajeICE(Long porcentajeICE) {
		this.porcentajeICE = porcentajeICE;
	}

	public Double getValorICE() {
		return valorICE;
	}

	public void setValorICE(Double valorICE) {
		this.valorICE = valorICE;
	}

	public Double getSubsidio() {
		return subsidio;
	}

	public void setSubsidio(Double subsidio) {
		this.subsidio = subsidio;
	}

	public Double getPrecioSinSub() {
		return precioSinSub;
	}

	public void setPrecioSinSub(Double precioSinSub) {
		this.precioSinSub = precioSinSub;
	}

	public Double getTotal() {
		return total;
	}

	public void setTotal(Double total) {
		this.total = total;
	}

	public ProductoCobro getProducto() {
		return producto;
	}

	public void setProducto(ProductoCobro producto) {
		this.producto = producto;
	}

	public Long getCodigoIVASRI() {
		return codigoIVASRI;
	}

	public void setCodigoIVASRI(Long codigoIVASRI) {
		this.codigoIVASRI = codigoIVASRI;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}
}
