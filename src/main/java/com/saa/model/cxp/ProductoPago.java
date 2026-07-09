/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.cxp;

import java.io.Serializable;

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

/**
*  @author GaemiSoft
*  Pojo mapeo de tabla PGS.PRDP.
*  Entity ProductoPago.
*  Tabla que contiene los productos para facturación electrónica.
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "PRDP", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "ProductoPagoAll", query = "select e from ProductoPago e"),
	@NamedQuery(name = "ProductoPagoId", query = "select e from ProductoPago e where e.id = :id")
})
public class ProductoPago implements Serializable {

	/**
	 * ID del producto
	 */
	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Facturador al que pertenece el producto
	 */
	@ManyToOne
	@JoinColumn(name = "EMPRESA", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	/**
	 * Grupo de producto al que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "GRUPOPRODUCTO", referencedColumnName = "GRPPCDGO")
	private GrupoProductoPago grupoProducto;
	
	/**
	 * Nombre del producto
	 */
	@Basic
	@Column(name = "NOMBRE", length = 1000)
	private String nombre;
	
	/**
	 * Código que utiliza el facturador para este producto
	 */
	@Basic
	@Column(name = "CODIGO", length = 500)
	private String codigo;
	
	/**
	 * Código auxiliar del producto
	 */
	@Basic
	@Column(name = "CODIGOAUX", length = 500)
	private String codigoAux;
	
	/**
	 * Precio unitario del producto
	 */
	@Basic
	@Column(name = "PRECIOUNITARIO")
	private Double precioUnitario;
	
	/**
	 * Descuento aplicable
	 */
	@Basic
	@Column(name = "DESCUENTO")
	private Double descuento;
	
	/**
	 * Tipo de descuento: 0 - Valor, 1 - Porcentaje
	 */
	@Basic
	@Column(name = "TIPODESCUENTO")
	private Long tipoDescuento;
	
	/**
	 * Indica si el precio incluye IVA (1=Sí, 0=No)
	 */
	@Basic
	@Column(name = "INCLUYEIVA")
	private Long incluyeIVA;
	
	/**
	 * Tipo de IVA aplicable
	 */
	@Basic
	@Column(name = "TIPOIVA")
	private Long tipoIVA;
	
	/**
	 * Tipo de ICE aplicable
	 */
	@Basic
	@Column(name = "TIPOICE")
	private Long tipoICE;
	
	/**
	 * Valor del ICE
	 */
	@Basic
	@Column(name = "ICE")
	private Double ice;
	
	/**
	 * Descripción del producto
	 */
	@Basic
	@Column(name = "DESCRIPCION", length = 1000)
	private String descripcion;
	
	/**
	 * Subsidio aplicable
	 */
	@Basic
	@Column(name = "SUBSIDIO")
	private Double subsidio;
	
	/**
	 * Precio sin subsidio
	 */
	@Basic
	@Column(name = "PRECIOSINSUB")
	private Double precioSinSub;
	
	/**
	 * IRBPNR (Impuesto Redimible Botellas Plásticas No Retornables)
	 */
	@Basic
	@Column(name = "IRBPNR")
	private Double irbpnr;
	
	/**
	 * Maneja múltiples precios: 0=NO, 1=SI
	 */
	@Basic
	@Column(name = "MULTIPRECIO")
	private Long multiPrecio;
	
	/**
	 * Stock disponible del producto
	 */
	@Basic
	@Column(name = "STOCK")
	private Long stock;
	
	/**
	 * Indica si maneja unidad de medida
	 */
	@Basic
	@Column(name = "MANEJAUNIDAD")
	private Long manejaUnidad;
	
	/**
	 * Unidad de medida
	 */
	@Basic
	@Column(name = "UNIDAD")
	private Long unidad;
	
	/**
	 * Estado del producto
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

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public GrupoProductoPago getGrupoProducto() {
		return grupoProducto;
	}

	public void setGrupoProducto(GrupoProductoPago grupoProducto) {
		this.grupoProducto = grupoProducto;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getCodigoAux() {
		return codigoAux;
	}

	public void setCodigoAux(String codigoAux) {
		this.codigoAux = codigoAux;
	}

	public Double getPrecioUnitario() {
		return precioUnitario;
	}

	public void setPrecioUnitario(Double precioUnitario) {
		this.precioUnitario = precioUnitario;
	}

	public Double getDescuento() {
		return descuento;
	}

	public void setDescuento(Double descuento) {
		this.descuento = descuento;
	}

	public Long getTipoDescuento() {
		return tipoDescuento;
	}

	public void setTipoDescuento(Long tipoDescuento) {
		this.tipoDescuento = tipoDescuento;
	}

	public Long getIncluyeIVA() {
		return incluyeIVA;
	}

	public void setIncluyeIVA(Long incluyeIVA) {
		this.incluyeIVA = incluyeIVA;
	}

	public Long getTipoIVA() {
		return tipoIVA;
	}

	public void setTipoIVA(Long tipoIVA) {
		this.tipoIVA = tipoIVA;
	}

	public Long getTipoICE() {
		return tipoICE;
	}

	public void setTipoICE(Long tipoICE) {
		this.tipoICE = tipoICE;
	}

	public Double getIce() {
		return ice;
	}

	public void setIce(Double ice) {
		this.ice = ice;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
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

	public Double getIrbpnr() {
		return irbpnr;
	}

	public void setIrbpnr(Double irbpnr) {
		this.irbpnr = irbpnr;
	}

	public Long getMultiPrecio() {
		return multiPrecio;
	}

	public void setMultiPrecio(Long multiPrecio) {
		this.multiPrecio = multiPrecio;
	}

	public Long getStock() {
		return stock;
	}

	public void setStock(Long stock) {
		this.stock = stock;
	}

	public Long getManejaUnidad() {
		return manejaUnidad;
	}

	public void setManejaUnidad(Long manejaUnidad) {
		this.manejaUnidad = manejaUnidad;
	}

	public Long getUnidad() {
		return unidad;
	}

	public void setUnidad(Long unidad) {
		this.unidad = unidad;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}
}
