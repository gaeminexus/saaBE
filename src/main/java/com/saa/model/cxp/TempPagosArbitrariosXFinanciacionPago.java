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
*  @author GaemiSoft
*  Pojo mapeo de tabla PGS.TPFP.
*  Entity TempPagosArbitrariosXFinanciacionPago.
*  Pagos arbitrarios de financiacion de pago 
*   en caso de que no se financie en pagos periodicos sino en pagos arbitrarios.
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "TPFP", schema = "PGS")
@SequenceGenerator(name = "SQ_TPFPCDGO", sequenceName = "PGS.SQ_TPFPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempPagosArbitrariosXFinanciacionPagoAll", query = "select e from TempPagosArbitrariosXFinanciacionPago e"),
	@NamedQuery(name = "TempPagosArbitrariosXFinanciacionPagoId", query = "select e from TempPagosArbitrariosXFinanciacionPago e where e.codigo = :id")
})
public class TempPagosArbitrariosXFinanciacionPago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "TPFPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TPFPCDGO")
	private Long codigo;
	
	/**
	 * Financiacion a la que pertenece la cuota inicial.
	 */
	@ManyToOne
	@JoinColumn(name = "TFDPCDGO", referencedColumnName = "TFDPCDGO")
	private TempFinanciacionXDocumentoPago tempFinanciacionXDocumentoPago;
	
	/**
	 * Numero de dia del mes en que se debe pagar. 
	 */
	@Basic
	@Column(name = "TPFPDAAA")
	private Long diaPago;
	
	/**
	 * Numero de mes en que se debe pagar.  
	 */
	@Basic
	@Column(name = "TPFPMSSS")
	private Long mesPago;
	
	/**
	 * Numero de anio en que se debe pagar.  
	 */
	@Basic
	@Column(name = "TPFPANOO")
	private Long anioPago;
	
	/**
	 * Fecha en la que se debe realizar el pago.  
	 */
	@Basic
	@Column(name = "TPFPFCPG")
	@Temporal(TemporalType.DATE)
	private Date fechaPago;
	
	/**
	 * Valor del pago.
	 */
	@Basic
	@Column(name = "TPFPVLRR")
	private Double valor;
	
	/**
	 * Obtiene codigo
	 */
	public Long getCodigo(){
		return codigo;
	}
	
	/**
	 * Asigna para el codigo.
	 */
	public void setCodigo(Long codigo){
		this.codigo = codigo;
	}
	
	/**
	 * Obtiene la Financiacion a la que pertenece la cuota inicial.
	 * @return : Financiacion a la que pertenece la cuota inicial.
	 */
	public TempFinanciacionXDocumentoPago getTempFinanciacionXDocumentoPago() {
		return tempFinanciacionXDocumentoPago;
	}

	/**
	 * Asigna la Financiacion a la que pertenece la cuota inicial.
	 * @param tempFinanciacionXDocumentoPago : Financiacion a la que pertenece la cuota inicial.
	 */
	public void setTempFinanciacionXDocumentoPago(
			TempFinanciacionXDocumentoPago tempFinanciacionXDocumentoPago) {
		this.tempFinanciacionXDocumentoPago = tempFinanciacionXDocumentoPago;
	}
	
	/**
	 * Obtiene el Numero de dia del mes en que se debe pagar. 
	 * @return : Numero de dia del mes en que se debe pagar. 
	 */
	public Long getDiaPago() {
		return diaPago;
	}

	/**
	 * Asigna el Numero de dia del mes en que se debe pagar. 
	 * @param diaPago : Numero de dia del mes en que se debe pagar. 
	 */
	public void setDiaPago(Long diaPago) {
		this.diaPago = diaPago;
	}

	/**
	 * Obtiene el Numero de mes en que se debe pagar.  
	 * @return : Numero de mes en que se debe pagar.  
	 */
	public Long getMesPago() {
		return mesPago;
	}

	/**
	 * Asigna el Numero de mes en que se debe pagar.  
	 * @param mesPago : Numero de mes en que se debe pagar.  
	 */
	public void setMesPago(Long mesPago) {
		this.mesPago = mesPago;
	}

	/**
	 * Obtiene el Numero de anio en que se debe pagar.  
	 * @return : Numero de anio en que se debe pagar.  
	 */
	public Long getAnioPago() {
		return anioPago;
	}

	/**
	 * Asigna el Numero de anio en que se debe pagar.  
	 * @param anioPago : Numero de anio en que se debe pagar.  
	 */
	public void setAnioPago(Long anioPago) {
		this.anioPago = anioPago;
	}

	/**
	 * Obtiene la Fecha en la que se debe realizar el pago.  
	 * @return : Fecha en la que se debe realizar el pago.  
	 */
	public Date getFechaPago() {
		return fechaPago;
	}

	/**
	 * Asigna la Fecha en la que se debe realizar el pago.  
	 * @param fechaPago : Fecha en la que se debe realizar el pago.  
	 */
	public void setFechaPago(Date fechaPago) {
		this.fechaPago = fechaPago;
	}

	/**
	 * Obtiene Valor de que se incluirá en la cuota inicial. 
	 * Puede ser el total o una parte del total de uno de los valores del resumen de valores del documento.    
	 * @return : Valor de que se incluirá en la cuota inicial. 
	 * Puede ser el total o una parte del total de uno de los valores del resumen de valores del documento.       
	 */
	public Double getValor() {
		return valor;
	}

	/**
	 * Asigna Valor de que se incluirá en la cuota inicial. 
	 * Puede ser el total o una parte del total de uno de los valores del resumen de valores del documento.       
	 * @param valor : Valor de que se incluirá en la cuota inicial. 
	 * Puede ser el total o una parte del total de uno de los valores del resumen de valores del documento.       
	 */
	public void setValor(Double valor) {
		this.valor = valor;
	}

}