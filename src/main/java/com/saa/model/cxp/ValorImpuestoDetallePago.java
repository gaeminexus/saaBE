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
*  Pojo mapeo de tabla PGS.VITP.
*  Entity ValorImpuestoDetallePago.
*  Valores de impuestos que se aplicaron a un detalle de documento de pago. 
*  Se llena de acuerdo a las reglas del la entidad Impuestos por Producto (pgs.ixpr).
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "VITP", schema = "PGS")
@SequenceGenerator(name = "SQ_VITPCDGO", sequenceName = "PGS.SQ_VITPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "ValorImpuestoDetallePagoAll", query = "select e from ValorImpuestoDetallePago e"),
	@NamedQuery(name = "ValorImpuestoDetallePagoId", query = "select e from ValorImpuestoDetallePago e where e.codigo = :id")
})
public class ValorImpuestoDetallePago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "VITPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_VITPCDGO")
	private Long codigo;
	
	/**
	 * Detalle de documento de pago al que pertenecen los valores.
	 */
	@ManyToOne
	@JoinColumn(name = "DTDPCDGO", referencedColumnName = "DTDPCDGO")
	private DetalleDocumentoPago detalleDocumentoPago;
	
	/**
	 * Detalle de impuesto aplicado.
	 
	@ManyToOne
	@JoinColumn(name = "DTIMCDGO", referencedColumnName = "DTIMCDGO")
	private DetalleImpuesto detalleImpuesto;
	*/
	/**
	 * Nombre del impuesto aplicado.
	 */
	@Basic
	@Column(name = "VITPNMBR")
	private String nombre;	
	
	/**
	 * Porcentaje aplicado como impuesto.
	 */
	@Basic
	@Column(name = "VITPPRCT")
	private Double porcentaje;
	
	/**
	 * Valor sobre el que se aplico el impuesto.   
	 */
	@Basic
	@Column(name = "VITPVLSA")
	private Double valorBase;
	
	/**
	 * Valor del impuesto.   
	 */
	@Basic
	@Column(name = "VITPVLRR")
	private Double valor;	
	
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
	 * Obtiene Detalle de documento de pago al que pertenecen los valores.
	 * @return : Detalle de documento de pago al que pertenecen los valores.
	 */
	public DetalleDocumentoPago getDetalleDocumentoPago() {
		return detalleDocumentoPago;
	}

	/**
	 * Asigna Detalle de documento de pago al que pertenecen los valores.
	 * @param detalleDocumentoPago : Detalle de documento de pago al que pertenecen los valores.
	 */
	public void setDetalleDocumentoPago(DetalleDocumentoPago detalleDocumentoPago) {
		this.detalleDocumentoPago = detalleDocumentoPago;
	}

	/**
	 * Obtine Detalle de impuesto aplicado.
	 * @return : Detalle de impuesto aplicado.
	 
	public DetalleImpuesto getDetalleImpuesto() {
		return detalleImpuesto;
	}
	*/

	/**
	 * Asigna Detalle de impuesto aplicado.
	 * @param detalleImpuesto : Detalle de impuesto aplicado.
	
	public void setDetalleImpuesto(DetalleImpuesto detalleImpuesto) {
		this.detalleImpuesto = detalleImpuesto;
	}
	*/

	/**
	 * Obtiene Nombre del impuesto aplicado.
	 * @return : Nombre del impuesto aplicado.
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna Nombre del impuesto aplicado.
	 * @param nombre : Nombre del impuesto aplicado.
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Obtiene Porcentaje aplicado como impuesto.
	 * @return : Porcentaje aplicado como impuesto.
	 */
	public Double getPorcentaje() {
		return porcentaje;
	}

	/**
	 * Asigna Porcentaje aplicado como impuesto.
	 * @param porcentaje : Porcentaje aplicado como impuesto.
	 */
	public void setPorcentaje(Double porcentaje) {
		this.porcentaje = porcentaje;
	}

	/**
	 * Obtiene Valor sobre el que se aplico el impuesto.
	 * @return : Valor sobre el que se aplico el impuesto.   
	 */
	public Double getValorBase() {
		return valorBase;
	}

	/**
	 * Asigna Valor sobre el que se aplico el impuesto.   
	 * @param valorBase : Valor sobre el que se aplico el impuesto.   
	 */
	public void setValorBase(Double valorBase) {
		this.valorBase = valorBase;
	}

	/**
	 * Obtiene Valor del impuesto.
	 * @return : Valor del impuesto.   
	 */
	public Double getValor() {
		return valor;
	}

	/**
	 * Asigna Valor del impuesto.   
	 * @param valor : Valor del impuesto.   
	 */
	public void setValor(Double valor) {
		this.valor = valor;
	}

}