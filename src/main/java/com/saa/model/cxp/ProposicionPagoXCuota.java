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
*  Pojo mapeo de tabla PGS.PRPD.
*  Entity ProposicionPagoXCuota.
*  Proposicion pago de pago de deposito.
*/
@SuppressWarnings("serial")
@Entity
@Table(name = "PRPD", schema = "PGS")
@SequenceGenerator(name = "SQ_PRPDCDGO", sequenceName = "PGS.SQ_PRPDCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "ProposicionPagoXCuotaAll", query = "select e from ProposicionPagoXCuota e"),
	@NamedQuery(name = "ProposicionPagoXCuotaId", query = "select e from ProposicionPagoXCuota e where e.codigo = :id")
})
public class ProposicionPagoXCuota implements Serializable {

	/**
	 * Codigo de la entidad.
	 */
	@Id
	@Column(name = "PRPDCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PRPDCDGO")
	private Long codigo;
	
	/**
	 * Cuota a la que pertenece la proposicion.
	 */
	@ManyToOne
	@JoinColumn(name = "CXDPCDGO", referencedColumnName = "CXDPCDGO")
	private CuotaXFinanciacionPago cuotaXFinanciacionPago;
	
	/**
	 * Valor de la cuota.
	 */
	@Basic
	@Column(name = "PRPDVLCT")
	private Double valorCuota;
	
	/**
	 * Valor propuesto a pagar de la cuota.
	 */
	@Basic
	@Column(name = "PRPDVLRR")
	private Double valorPropuesto;
	
	/**
	 * Fecha de generacion o ingreso de la cuota.
	 */
	@Basic
	@Column(name = "PRPDFCIN")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaIngreso;
	
	/**
	 * Tipo de proposicion. 1 = abono, 2 = pago total.
	 */
	@Basic
	@Column(name = "PRPDTPPR")
	private Long tipo;
	
	/**
	 * Numero de abono.
	 */
	@Basic
	@Column(name = "PRPDNMAB")
	private Long numeroAbono;
	
	/**
	 * Estado de la proposicion. 1 = activo, 2 = anulado, 3 = procesado.
	 */
	@Basic
	@Column(name = "PRPDESTD")
	private Long estado;
	
	/**
	 * Fecha de pago de la proposición. Control para impresion de pago.
	 */
	@Basic
	@Column(name = "PRPDFCPG")
	@Temporal(TemporalType.DATE)
	private Date fechaPago;
	
	/**
	 * Nombre usuario que ingresa la proposicion.
	 */
	@Basic
	@Column(name = "PRPDUSPR")
	private String nombreUsuario;
	
	/**
	 * Numero de aprobaciones realizadas sobre la proposicion. 
	 * Con máximo nivel de aprobacion pasa a pagos.
	 */
	@Basic
	@Column(name = "PRPDNMAP")
	private Long aprobacionesRealizadas;
	
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
	 * Obtiene Cuota a la que pertenece la proposicion.
	 * @return : Cuota a la que pertenece la proposicion.
	 */
	public CuotaXFinanciacionPago getCuotaXFinanciacionPago() {
		return cuotaXFinanciacionPago;
	}

	/**
	 * Asigna Cuota a la que pertenece la proposicion.
	 * @param cuotaXFinanciacionPago : Cuota a la que pertenece la proposicion.
	 */
	public void setCuotaXFinanciacionPago(
			CuotaXFinanciacionPago cuotaXFinanciacionPago) {
		this.cuotaXFinanciacionPago = cuotaXFinanciacionPago;
	}

	/**
	 * Obtiene Valor de la cuota.
	 * @return : Valor de la cuota.
	 */
	public Double getValorCuota() {
		return valorCuota;
	}

	/**
	 * Asigna Valor de la cuota.
	 * @param valorCuota : Valor de la cuota.
	 */
	public void setValorCuota(Double valorCuota) {
		this.valorCuota = valorCuota;
	}

	/**
	 * Obtiene Valor propuesto a pagar de la cuota.
	 * @return : Valor propuesto a pagar de la cuota.
	 */
	public Double getValorPropuesto() {
		return valorPropuesto;
	}

	/**
	 * Asigna Valor propuesto a pagar de la cuota.
	 * @param valorPropuesto : Valor propuesto a pagar de la cuota.
	 */
	public void setValorPropuesto(Double valorPropuesto) {
		this.valorPropuesto = valorPropuesto;
	}

	/**
	 * Obtiene Fecha de generacion o ingreso de la cuota.
	 * @return : Fecha de generacion o ingreso de la cuota.
	 */
	public Date getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna Fecha de generacion o ingreso de la cuota.
	 * @param fechaIngreso : Fecha de generacion o ingreso de la cuota.
	 */
	public void setFechaIngreso(Date fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	/**
	 * Obtiene Tipo de proposicion. 1 = abono, 2 = pago total.
	 * @return : Tipo de proposicion. 1 = abono, 2 = pago total.
	 */
	public Long getTipo() {
		return tipo;
	}

	/**
	 * Asigna Tipo de proposicion. 1 = abono, 2 = pago total.
	 * @param tipo : Tipo de proposicion. 1 = abono, 2 = pago total.
	 */
	public void setTipo(Long tipo) {
		this.tipo = tipo;
	}

	/**
	 * Obtiene Numero de abono. 
	 * @return : Numero de abono.
	 */
	public Long getNumeroAbono() {
		return numeroAbono;
	}

	/**
	 * Asigna Numero de abono.
	 * @param numeroAbono : Numero de abono.
	 */
	public void setNumeroAbono(Long numeroAbono) {
		this.numeroAbono = numeroAbono;
	}

	/**
	 * Obtiene Estado de la proposicion. 1 = activo, 2 = anulado, 3 = procesado.
	 * @return : Estado de la proposicion. 1 = activo, 2 = anulado, 3 = procesado.
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna Estado de la proposicion. 1 = activo, 2 = anulado, 3 = procesado.
	 * @param estado : Estado de la proposicion. 1 = activo, 2 = anulado, 3 = procesado.
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

	/**
	 * Obtiene Fecha de pago de la proposición. Control para impresion de pago.
	 * @return : Fecha de pago de la proposición. Control para impresion de pago.
	 */
	public Date getFechaPago() {
		return fechaPago;
	}

	/**
	 * Asigna Fecha de pago de la proposición. Control para impresion de pago.
	 * @param fechaPago : Fecha de pago de la proposición. Control para impresion de pago.
	 */
	public void setFechaPago(Date fechaPago) {
		this.fechaPago = fechaPago;
	}

	/**
	 * Obtiene Nombre usuario que ingresa la proposicion.
	 * @return : Nombre usuario que ingresa la proposicion.
	 */
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	/**
	 * Asigna Nombre usuario que ingresa la proposicion.
	 * @param nombreUsuario : Nombre usuario que ingresa la proposicion.
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}

	/**
	 * Obtiene Numero de aprobaciones realizadas sobre la proposicion. 
	 * Con máximo nivel de aprobacion pasa a pagos.
	 * @return : Numero de aprobaciones realizadas sobre la proposicion. 
	 * Con máximo nivel de aprobacion pasa a pagos.
	 */
	public Long getAprobacionesRealizadas() {
		return aprobacionesRealizadas;
	}

	/**
	 * Asigna Numero de aprobaciones realizadas sobre la proposicion. 
	 * Con máximo nivel de aprobacion pasa a pagos.
	 * @param aprobacionesRealizadas : Numero de aprobaciones realizadas sobre la proposicion. 
	 * Con máximo nivel de aprobacion pasa a pagos.
	 */
	public void setAprobacionesRealizadas(Long aprobacionesRealizadas) {
		this.aprobacionesRealizadas = aprobacionesRealizadas;
	}

}