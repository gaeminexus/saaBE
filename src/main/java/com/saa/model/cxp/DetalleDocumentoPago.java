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
*  @author GaemiSoft
*  Pojo mapeo de tabla PGS.DTDP.
*  Entity DetalleDocumentoPago.
*  Detalle de documento en el que se almancenan todos los productos incluidos en el documento.
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "DTDP", schema = "PGS")
// @SequenceGenerator(name = "SQ_DTDPCDGO", sequenceName = "PGS.SQ_DTDPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DetalleDocumentoPagoAll", query = "select e from DetalleDocumentoPago e"),
	@NamedQuery(name = "DetalleDocumentoPagoId", query = "select e from DetalleDocumentoPago e where e.codigo = :id")
})
public class DetalleDocumentoPago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "DTDPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTDPCDGO")
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
	@JoinColumn(name = "DCMPCDGO", referencedColumnName = "DCMPCDGO")
	private DocumentoPago documentoPago;
	
	/**
	 * Producto incluido en el detalle tomado de la tabla de productos.
	 */
	@ManyToOne
	@JoinColumn(name = "PRDPCDGO", referencedColumnName = "PRDPCDGO")
	private ProductoPago productoPago;
	
	/**
	 * Descripcion del detalle.
	 */
	@Basic
	@Column(name = "DTDPDSCR")
	private String descripcion;	
	
	/**
	 * Cantidad de productos incluidos en el detalle.
	 */
	@Basic
	@Column(name = "DTDPCNTD")
	private Double cantidad;
	
	/**
	 * Precio unitario del producto.   
	 */
	@Basic
	@Column(name = "DTDPPRUN")
	private Double precioUnitario;
	
	/**
	 * Subtotal = cantidad por precio unitario.   
	 */
	@Basic
	@Column(name = "DTDPSBTT")
	private Double subtotal;
	
	/**
	 * Total de impuestos que se aplican al producto. 
	 */
	@Basic
	@Column(name = "DTDPTTIM")
	private Double totalImpuesto;
	
	/**
	 * Total = subtota mas impuestos. 
	 */
	@Basic
	@Column(name = "DTDPTTLL")
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
	@Column(name = "DTDPNMLN")
	private Long numeroLinea;
	
	/**
	 * Estado del detalle de documento. 1 = activo, 2 = anulado
	 */
	@Basic
	@Column(name = "DTDPESTD")
	private Long estado;
	
	/**
	 * Fecha de ingreso.
	 */
	@Basic
	@Column(name = "DTDPFCIN")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaIngreso;
	
	/**
	 * Obtiene codigo
	 */
	public Long getCodigo(){
		return codigo;
	}
	
	/**
	 * Asigna para el codigo
	 */
	public void setCodigo(Long codigo){
		this.codigo = codigo;
	}
	
	/**
	 * Obtiene la empresa a la que pertenece el grupo de productos
	 * @return : Empresa a la que pertenece el grupo de productos
	 */	
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna la empresa a la que pertenece el grupo de productos
	 * @param empresa : Empresa a la que pertenece el grupo de productos
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	/**
	 * Obtiene Documento al que pertenece el detalle.
	 * @return : Documento al que pertenece el detalle.
	 */
	public DocumentoPago getDocumentoPago() {
		return documentoPago;
	}

	/**
	 * Asigna Documento al que pertenece el detalle.
	 * @param documentoPago : Documento al que pertenece el detalle. 
	 */
	public void setDocumentoPago(DocumentoPago documentoPago) {
		this.documentoPago = documentoPago;
	}

	/**
	 * Obtiene Producto incluido en el detalle tomado de la tabla de productos.
	 * @return : Producto incluido en el detalle tomado de la tabla de productos.
	 */
	public ProductoPago getProductoPago() {
		return productoPago;
	}

	/**
	 * Asigna Producto incluido en el detalle tomado de la tabla de productos.
	 * @param productoPago : Producto incluido en el detalle tomado de la tabla de productos.
	 */
	public void setProductoPago(ProductoPago productoPago) {
		this.productoPago = productoPago;
	}

	/**
	 * Obtiene Descripcion del detalle.
	 * @return : Descripcion del detalle.
	 */	
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Asigna Descripcion del detalle.
	 * @param descripcion : Descripcion del detalle.
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	/**
	 * Obtiene Cantidad de productos incluidos en el detalle.
	 * @return : Cantidad de productos incluidos en el detalle.
	 */
	public Double getCantidad() {
		return cantidad;
	}

	/**
	 * Asigna Cantidad de productos incluidos en el detalle.
	 * @param cantidad : Cantidad de productos incluidos en el detalle.
	 */
	public void setCantidad(Double cantidad) {
		this.cantidad = cantidad;
	}

	/**
	 * Obtiene Precio unitario del producto.
	 * @return : Precio unitario del producto.
	 */	
	public Double getPrecioUnitario() {
		return precioUnitario;
	}

	/**
	 * Asigna Precio unitario del producto.
	 * @param precioUnitario : Precio unitario del producto.
	 */
	public void setPrecioUnitario(Double precioUnitario) {
		this.precioUnitario = precioUnitario;
	}

	/**
	 * Obtiene Subtotal = cantidad por precio unitario.
	 * @return : Subtotal = cantidad por precio unitario.
	 */
	public Double getSubtotal() {
		return subtotal;
	}

	/**
	 * Asigna Subtotal = cantidad por precio unitario.
	 * @param subtotal : Subtotal = cantidad por precio unitario.
	 */
	public void setSubtotal(Double subtotal) {
		this.subtotal = subtotal;
	}

	/**
	 * Obtiene Total de impuestos que se aplican al producto.  
	 * @return : Total de impuestos que se aplican al producto. 
	 */	
	public Double getTotalImpuesto() {
		return totalImpuesto;
	}

	/**
	 * Asigna Total de impuestos que se aplican al producto. 
	 * @param totalImpuesto : Total de impuestos que se aplican al producto. 
	 */
	public void setTotalImpuesto(Double totalImpuesto) {
		this.totalImpuesto = totalImpuesto;
	}

	/**
	 * Obtiene Total = subtota mas impuestos. 
	 * @return : Total = subtota mas impuestos. 
	 */
	public Double getTotal() {
		return total;
	}

	/**
	 * Asigna Total = subtota mas impuestos. 
	 * @param total : Total = subtota mas impuestos. 
	 */
	public void setTotal(Double total) {
		this.total = total;
	}

	/**
	 * Obtiene Centro de costo que se aplica en caso de que maneje centro de costo. 
	 * @return : Centro de costo que se aplica en caso de que maneje centro de costo. 
	 */
	public CentroCosto getCentroCosto() {
		return centroCosto;
	}

	/**
	 * Asigna Centro de costo que se aplica en caso de que maneje centro de costo. 
	 * @param centroCosto : Centro de costo que se aplica en caso de que maneje centro de costo. 
	 */
	public void setCentroCosto(CentroCosto centroCosto) {
		this.centroCosto = centroCosto;
	}

	/**
	 * Obtiene Numero de linea de este detalle dentro del documento. 
	 * @return : Numero de linea de este detalle dentro del documento. 
	 */
	public Long getNumeroLinea() {
		return numeroLinea;
	}

	/**
	 * Asigna Numero de linea de este detalle dentro del documento. 
	 * @param numeroLinea : Numero de linea de este detalle dentro del documento. 
	 */
	public void setNumeroLinea(Long numeroLinea) {
		this.numeroLinea = numeroLinea;
	}

	/**
	 * Obtiene Estado del detalle de documento. 1 = activo, 2 = anulado
	 * @return : Estado del detalle de documento. 1 = activo, 2 = anulado
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna Estado del detalle de documento. 1 = activo, 2 = anulado
	 * @param estado : Estado del detalle de documento. 1 = activo, 2 = anulado
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

	/**
	 * Obtiene Fecha de ingreso.
	 * @return : Fecha de ingreso.
	 */	
	public Date getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna Fecha de ingreso.
	 * @param fechaIngreso : Fecha de ingreso.
	 */
	public void setFechaIngreso(Date fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}	
		
}