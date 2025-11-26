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
*  Pojo mapeo de tabla PGS.RVDP.
*  Entity ResumenValorDocumentoPago.
*  Resumen de valores de documento de pago.
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "RVDP", schema = "PGS")
@SequenceGenerator(name = "SQ_RVDPCDGO", sequenceName = "PGS.SQ_RVDPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "ResumenValorDocumentoPagoAll", query = "select e from ResumenValorDocumentoPago e"),
	@NamedQuery(name = "ResumenValorDocumentoPagoId", query = "select e from ResumenValorDocumentoPago e where e.codigo = :id")
})
public class ResumenValorDocumentoPago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "RVDPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_RVDPCDGO")
	private Long codigo;
	
	/**
	 * Documento de pago al que pertenecen los valores.
	 */
	@ManyToOne
	@JoinColumn(name = "DCMPCDGO", referencedColumnName = "DCMPCDGO")
	private DocumentoPago documentoPago;
	
	/**
	 * Codigo alterno del tipo de valor aplicado. 
	 * Tomado de la entidad ValoresXDocumento(pgs.vxdc).
	 */
	@Basic
	@Column(name = "RVDPCATP")
	private Long codigoAlternoTipoValor;
	
	/**
	 * Valor del impuesto.   
	 */
	@Basic
	@Column(name = "RVDPVLRR")
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
	 * Obtiene Documento de pago al que pertenecen los valores.
	 * @return : Documento de pago al que pertenecen los valores.
	 */
	public DocumentoPago getDocumentoPago() {
		return documentoPago;
	}

	/**
	 * Asigna Documento de pago al que pertenecen los valores.
	 * @param documentoPago : Documento de pago al que pertenecen los valores.
	 */
	public void setDocumentoPago(DocumentoPago documentoPago) {
		this.documentoPago = documentoPago;
	}

	/**
	 * Obtiene Codigo alterno del tipo de valor aplicado. 
	 * Tomado de la entidad ValoresXDocumento(pgs.vxdc).
	 * @return : Codigo alterno del tipo de valor aplicado. 
	 * Tomado de la entidad ValoresXDocumento(pgs.vxdc).
	 */
	public Long getCodigoAlternoTipoValor() {
		return codigoAlternoTipoValor;
	}

	/**
	 * Asigna Codigo alterno del tipo de valor aplicado. 
	 * Tomado de la entidad ValoresXDocumento(pgs.vxdc).
	 * @param codigoAlternoTipoValor : Codigo alterno del tipo de valor aplicado. 
	 * Tomado de la entidad ValoresXDocumento(pgs.vxdc).
	 */
	public void setCodigoAlternoTipoValor(Long codigoAlternoTipoValor) {
		this.codigoAlternoTipoValor = codigoAlternoTipoValor;
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