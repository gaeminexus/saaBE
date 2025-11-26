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
*  @author GaemiSoft
* <p>Pojo mapeo de tabla PGS.IXGP.
*  Entity ImpuestoXGrupoPago.
*  Contiene la parametrización de Impuestos por grupo de productos de pagos. 
*  Impuestos que aplica a los productos de los proveedores.
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "IXGP", schema = "PGS")
@SequenceGenerator(name = "SQ_IXGPCDGO", sequenceName = "PGS.SQ_IXGPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "ImpuestoXGrupoPagoAll", query = "select e from ImpuestoXGrupoPago e"),
	@NamedQuery(name = "ImpuestoXGrupoPagoId", query = "select e from ImpuestoXGrupoPago e where e.codigo = :id")
})
public class ImpuestoXGrupoPago implements Serializable {

	/**
	 * Codigo de la empresa
	 */
	@Id
	@Column(name = "IXGPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_IXGPCDGO")
	private Long codigo;	
	
	/**
	 * Grupo de producto al que se aplicará el impuesto. 
	 */
	@ManyToOne
	@JoinColumn(name = "GRPPCDGO", referencedColumnName = "GRPPCDGO")
	private GrupoProductoPago grupoProductoPago;	
	
	/**
	 * Detalle de impuesto que se aplicará al producto 
	@ManyToOne
	@JoinColumn(name = "DTIMCDGO", referencedColumnName = "DTIMCDGO")
	private DetalleImpuesto detalleImpuesto;
	*/
	
	/**
	 * Estado del impuesto por grupo. 1 = activo, 2 = inactivo.
	 */
	@Basic
	@Column(name = "IXGPESTD")
	private Long estado;
	
	/**
	 * Obtiene codigo
	 */
	public Long getCodigo(){
		return codigo;
	}
	
	/**
	 * Asigna el codigo
	 */
	public void setCodigo(Long codigo){
		this.codigo = codigo;
	}

	/**
	 * Obtiene el Grupo de producto al que se aplicará el impuesto. 
	 * @return : Grupo de producto al que se aplicará el impuesto. 
	 */
	public GrupoProductoPago getGrupoProductoPago() {
		return grupoProductoPago;
	}

	/**
	 * Asigna el Grupo de producto al que se aplicará el impuesto. 
	 * @param grupoProductoPago : Grupo de producto al que se aplicará el impuesto. 
	 */
	public void setGrupoProductoPago(GrupoProductoPago grupoProductoPago) {
		this.grupoProductoPago = grupoProductoPago;
	}

	/**
	 * Obtiene el Detalle de impuesto que se aplicará al producto.
	 * @return : Detalle de impuesto que se aplicará al producto.
	public DetalleImpuesto getDetalleImpuesto() {
		return detalleImpuesto;
	}
	*/
	
	/**
	 * Asigna el Detalle de impuesto que se aplicará al producto.
	 * @param detalleImpuesto : Detalle de impuesto que se aplicará al producto.
	public void setDetalleImpuesto(DetalleImpuesto detalleImpuesto) {
		this.detalleImpuesto = detalleImpuesto;
	}
	*/
	
	/**
	 * Obtiene el estado del impuesto por grupo. 1 = activo, 2 = inactivo.
	 * @return : Estado del impuesto por grupo. 1 = activo, 2 = inactivo.
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna el estado del impuesto por grupo. 1 = activo, 2 = inactivo.
	 * @param estado : Estado del impuesto por grupo. 1 = activo, 2 = inactivo.
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}
	
}