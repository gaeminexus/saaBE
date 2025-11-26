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
*  Pojo mapeo de tabla PGS.TCDP.
*  Entity TempCuotaXFinanciacionPago.
*  Cuotas que se deben cancelar del documento de pago. 
*  Son el resultado de la financiacion por lo que son hijas de la entidad de 
*   financiacion por documento de pago FXDP.
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "TCDP", schema = "PGS")
@SequenceGenerator(name = "SQ_TCDPCDGO", sequenceName = "PGS.SQ_TCDPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempCuotaXFinanciacionPagoAll", query = "select e from TempCuotaXFinanciacionPago e"),
	@NamedQuery(name = "TempCuotaXFinanciacionPagoId", query = "select e from TempCuotaXFinanciacionPago e where e.codigo = :id")
})
public class TempCuotaXFinanciacionPago implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "TCDPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCDPCDGO")
	private Long codigo;
	
	/**
	 * Financiacion a la que pertenece la cuota inicial.
	 */
	@ManyToOne
	@JoinColumn(name = "TFDPCDGO", referencedColumnName = "TFDPCDGO")
	private TempFinanciacionXDocumentoPago tempFinanciacionXDocumentoPago;
	
	/**
	 * Fecha de generacion o ingreso de la cuota.
	 */
	@Basic
	@Column(name = "TCDPFCIN")	
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaIngreso;
	
	/**
	 * Fecha de vencimiento de la cuota.
	 */
	@Basic
	@Column(name = "TCDPFCVN")	
	@Temporal(TemporalType.DATE)
	private Date fechaVencimiento;
	
	/**
	 * Tipo de pago. 1 = cuota, 2 = letra, 3 = cuota inicial.
	 */
	@Basic
	@Column(name = "TCDPTPOO")	
	private Long tipo;
	
	/**
	 * Valor a pagar por la cuota.    
	 */
	@Basic
	@Column(name = "TCDPVLRR")	
	private Double valor;
	
	/**
	 * Numero secuencial de cuota dentro del total de la financiación. 
	 */
	@Basic
	@Column(name = "TCDPNMSC")	
	private Long numeroSecuencial;
	
	/**
	 * Numero de cuota o letra. el numero de letra es un secuencial dentro de toda la empresa. 
	 */
	@Basic
	@Column(name = "TCDPNMCL")
	private Long numeroCuotaLetra;
	
	/**
	 * Numero total de cuotas en las que fue financiado el documento. 
	 */
	@Basic
	@Column(name = "TCDPNMTC")
	private Long numeroTotalCuotas;
	
	/**
	 * Total abonado a la cuota.   
	 */
	@Basic
	@Column(name = "TCDPABNO")
	private Double totalAbono;	
	
	/**
	 * Saldo de la cuota.
	 */
	@Basic
	@Column(name = "TCDPSLDO")
	private Double saldo;
	
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
	 * Obtiene la Fecha de generacion o ingreso de la cuota.
	 * @return : Fecha de generacion o ingreso de la cuota.
	 */
	public Date getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna la Fecha de generacion o ingreso de la cuota.
	 * @param fechaIngreso : Fecha de generacion o ingreso de la cuota.
	 */
	public void setFechaIngreso(Date fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	/**
	 * Obtiene la Fecha de vencimiento de la cuota.
	 * @return : Fecha de vencimiento de la cuota.
	 */
	public Date getFechaVencimiento() {
		return fechaVencimiento;
	}

	/**
	 * Asigna la Fecha de vencimiento de la cuota.
	 * @param fechaVencimiento : Fecha de vencimiento de la cuota.
	 */
	public void setFechaVencimiento(Date fechaVencimiento) {
		this.fechaVencimiento = fechaVencimiento;
	}

	/**
	 * Obtiene el Tipo de pago. 1 = cuota, 2 = letra, 3 = cuota inicial.
	 * @return : Tipo de pago. 1 = cuota, 2 = letra, 3 = cuota inicial.
	 */
	public Long getTipo() {
		return tipo;
	}

	/**
	 * Asigna el Tipo de pago. 1 = cuota, 2 = letra, 3 = cuota inicial.
	 * @param tipo : Tipo de pago. 1 = cuota, 2 = letra, 3 = cuota inicial.
	 */
	public void setTipo(Long tipo) {
		this.tipo = tipo;
	}

	/**
	 * Obtiene el Valor a pagar por la cuota.    
	 * @return : Valor a pagar por la cuota.    
	 */
	public Double getValor() {
		return valor;
	}

	/**
	 * Asigna el Valor a pagar por la cuota.    
	 * @param valor : Valor a pagar por la cuota.    
	 */
	public void setValor(Double valor) {
		this.valor = valor;
	}

	/**
	 * Obtiene el Numero secuencial de cuota dentro del total de la financiación.
	 * @return : Numero secuencial de cuota dentro del total de la financiación.
	 */
	public Long getNumeroSecuencial() {
		return numeroSecuencial;
	}

	/**
	 * Asigna el Numero secuencial de cuota dentro del total de la financiación.
	 * @param numeroSecuencial : Numero secuencial de cuota dentro del total de la financiación.
	 */
	public void setNumeroSecuencial(Long numeroSecuencial) {
		this.numeroSecuencial = numeroSecuencial;
	}

	/**
	 * Obtiene el Numero de cuota o letra. 
	 * El numero de letra es un secuencial dentro de toda la empresa.
	 * @return : Numero de cuota o letra. 
	 * El numero de letra es un secuencial dentro de toda la empresa. 
	 */
	public Long getNumeroCuotaLetra() {
		return numeroCuotaLetra;
	}

	/**
	 * Asigna el Numero de cuota o letra. 
	 * El numero de letra es un secuencial dentro de toda la empresa.
	 * @param numeroCuotaLetra : Numero de cuota o letra. 
	 * El numero de letra es un secuencial dentro de toda la empresa.
	 */
	public void setNumeroCuotaLetra(Long numeroCuotaLetra) {
		this.numeroCuotaLetra = numeroCuotaLetra;
	}

	/**
	 * Obtiene el Numero total de cuotas en las que fue financiado el documento. 
	 * @return : Numero total de cuotas en las que fue financiado el documento. 
	 */
	public Long getNumeroTotalCuotas() {
		return numeroTotalCuotas;
	}

	/**
	 * Asigna el Numero total de cuotas en las que fue financiado el documento. 
	 * @param numeroTotalCuotas : Numero total de cuotas en las que fue financiado el documento. 
	 */
	public void setNumeroTotalCuotas(Long numeroTotalCuotas) {
		this.numeroTotalCuotas = numeroTotalCuotas;
	}

	/**
	 * Obtiene el Total abonado a la cuota.
	 * @return : Total abonado a la cuota.
	 */
	public Double getTotalAbono() {
		return totalAbono;
	}

	/**
	 * Asigna el Total abonado a la cuota.
	 * @param totalAbono : Total abonado a la cuota.
	 */
	public void setTotalAbono(Double totalAbono) {
		this.totalAbono = totalAbono;
	}

	/**
	 * Obtiene el Saldo de la cuota.
	 * @return : Saldo de la cuota.
	 */
	public Double getSaldo() {
		return saldo;
	}

	/**
	 * Asigna el Saldo de la cuota.
	 * @param saldo : Saldo de la cuota.
	 */
	public void setSaldo(Double saldo) {
		this.saldo = saldo;
	}
	
}