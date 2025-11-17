/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tesoreria;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;

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
 * @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.CNCL.
 * Entity Conciliacion.
 * Almacena la cabecera de la conciliacion.
 * Se genera un registro por cuenta bancaria por periodo.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNCL", schema = "TSR")
@SequenceGenerator(name = "SQ_CNCLCDGO", sequenceName = "TSR.SQ_CNCLCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "ConciliacionAll", query = "select e from Conciliacion e"),
	@NamedQuery(name = "ConciliacionId", query = "select e from Conciliacion e where e.codigo = :id")
})
public class Conciliacion implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "CNCLCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CNCLCDGO")
	private Long codigo;
	
	/**
	 * Id del periodo contable en el que se realizo la conciliacion.
	 */
	@Basic
	@Column(name = "CNCLPRDO")
	private Long idPeriodo;
	
	/**
	 * Usuario que realiza la conciliacion.
	 */
	@ManyToOne
	@JoinColumn(name = "CNCLUSRR", referencedColumnName = "PJRQCDGO")
	private Usuario usuario;
	
	/**
	 * Fecha en que se realiza la conciliacion.
	 */
	@Basic
	@Column(name = "CNCLFCHA")
	private LocalDateTime fecha;
	
	/**
	 * Cuenta bancaria que se concilia
	 */
	@ManyToOne
	@JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
	private CuentaBancaria cuentaBancaria;	

	/**
	 * Saldo inicial segun los registros contables del sistema.
	 */
	@Basic
	@Column(name = "CNCLSILB")
	private Double inicialSistema;
	
	/**
	 * Total en depositos de la cuenta bancaria en periodo segun el sistema.
	 */
	@Basic
	@Column(name = "CNCLDPST")
	private Double depositoSistema;
	
	/**
	 * Total en notas de credito de la cuenta bancaria en periodo segun el sistema.
	 */
	@Basic
	@Column(name = "CNCLNCRD")
	private Double creditoSistema;
	
	/**
	 * Total en cheques emitidos de la cuenta bancaria en periodo segun el sistema.
	 */
	@Basic
	@Column(name = "CNCLCHQE")
	private Double chequeSistema;
	
	/**
	 * Total en notas de debito de la cuenta bancaria en periodo segun el sistema.
	 */
	@Basic
	@Column(name = "CNCLNTDB")
	private Double debitoSistema;
	
	/**
	 * Saldo final segun los registros contables del sistema.
	 */
	@Basic
	@Column(name = "CNCLSLDF")
	private Double finalSistema;
	
	/**
	 * Saldo Final segun el estado de cuenta del banco.
	 */
	@Basic
	@Column(name = "CNCLSLDE")
	private Double saldoEstadoCuenta;
	
	/**
	 * Total en depositos en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCLDPTR")
	private Double depositoTransito;
	
	/**
	 * Total en cheques en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCLCHNC")
	private Double chequeTransito;
	
	/**
	 * Total en notas de credito en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCLNCTR")
	private Double creditoTransito;
	
	/**
	 * Total en notas de debito en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCLNDTR")
	private Double debitoTransito;
	
	/**
	 * Saldo final del banco luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCLSLDB")
	private Double saldoBanco;
	
	/**
	 * Empresa en la que se realiza la conciliacion.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;	

	/**
	 * Rubro 43. Estados de conciliacion.
	 */
	@Basic
	@Column(name = "CNCLRYYA")
	private Long rubroEstadoP;
	
	/**
	 * Detalle de Rubro 43. Estados de conciliacion.
	 */
	@Basic
	@Column(name = "CNCLRZZA")
	private Long rubroEstadoH;
	
	/**
	 * Total en transferencias de debito en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCLTDTR")
	private Double transferenciaDebitoTransito;
	
	/**
	 * Total en transferencias de credito en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCLTCTR")
	private Double transferenciaCreditoTransito;
	
	/**
	 * Total en transferencias de debito de la cuenta bancaria en periodo segun sistema.
	 */
	@Basic
	@Column(name = "CNCLTRDB")
	private Double transferenciaDebitoSistema;
	
	/**
	 * Total en transferencias de credito de la cuenta bancaria en periodo segun sistema.
	 */
	@Basic
	@Column(name = "CNCLTRCR")
	
	
	private Double transferenciaCreditoSistema;

	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	public Long getIdPeriodo() {
		return idPeriodo;
	}

	public void setIdPeriodo(Long idPeriodo) {
		this.idPeriodo = idPeriodo;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public CuentaBancaria getCuentaBancaria() {
		return cuentaBancaria;
	}

	public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
		this.cuentaBancaria = cuentaBancaria;
	}

	public Double getInicialSistema() {
		return inicialSistema;
	}

	public void setInicialSistema(Double inicialSistema) {
		this.inicialSistema = inicialSistema;
	}

	public Double getDepositoSistema() {
		return depositoSistema;
	}

	public void setDepositoSistema(Double depositoSistema) {
		this.depositoSistema = depositoSistema;
	}

	public Double getCreditoSistema() {
		return creditoSistema;
	}

	public void setCreditoSistema(Double creditoSistema) {
		this.creditoSistema = creditoSistema;
	}

	public Double getChequeSistema() {
		return chequeSistema;
	}

	public void setChequeSistema(Double chequeSistema) {
		this.chequeSistema = chequeSistema;
	}

	public Double getDebitoSistema() {
		return debitoSistema;
	}

	public void setDebitoSistema(Double debitoSistema) {
		this.debitoSistema = debitoSistema;
	}

	public Double getFinalSistema() {
		return finalSistema;
	}

	public void setFinalSistema(Double finalSistema) {
		this.finalSistema = finalSistema;
	}

	public Double getSaldoEstadoCuenta() {
		return saldoEstadoCuenta;
	}

	public void setSaldoEstadoCuenta(Double saldoEstadoCuenta) {
		this.saldoEstadoCuenta = saldoEstadoCuenta;
	}

	public Double getDepositoTransito() {
		return depositoTransito;
	}

	public void setDepositoTransito(Double depositoTransito) {
		this.depositoTransito = depositoTransito;
	}

	public Double getChequeTransito() {
		return chequeTransito;
	}

	public void setChequeTransito(Double chequeTransito) {
		this.chequeTransito = chequeTransito;
	}

	public Double getCreditoTransito() {
		return creditoTransito;
	}

	public void setCreditoTransito(Double creditoTransito) {
		this.creditoTransito = creditoTransito;
	}

	public Double getDebitoTransito() {
		return debitoTransito;
	}

	public void setDebitoTransito(Double debitoTransito) {
		this.debitoTransito = debitoTransito;
	}

	public Double getSaldoBanco() {
		return saldoBanco;
	}

	public void setSaldoBanco(Double saldoBanco) {
		this.saldoBanco = saldoBanco;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public Long getRubroEstadoP() {
		return rubroEstadoP;
	}

	public void setRubroEstadoP(Long rubroEstadoP) {
		this.rubroEstadoP = rubroEstadoP;
	}

	public Long getRubroEstadoH() {
		return rubroEstadoH;
	}

	public void setRubroEstadoH(Long rubroEstadoH) {
		this.rubroEstadoH = rubroEstadoH;
	}

	public Double getTransferenciaDebitoTransito() {
		return transferenciaDebitoTransito;
	}

	public void setTransferenciaDebitoTransito(Double transferenciaDebitoTransito) {
		this.transferenciaDebitoTransito = transferenciaDebitoTransito;
	}

	public Double getTransferenciaCreditoTransito() {
		return transferenciaCreditoTransito;
	}

	public void setTransferenciaCreditoTransito(Double transferenciaCreditoTransito) {
		this.transferenciaCreditoTransito = transferenciaCreditoTransito;
	}

	public Double getTransferenciaDebitoSistema() {
		return transferenciaDebitoSistema;
	}

	public void setTransferenciaDebitoSistema(Double transferenciaDebitoSistema) {
		this.transferenciaDebitoSistema = transferenciaDebitoSistema;
	}

	public Double getTransferenciaCreditoSistema() {
		return transferenciaCreditoSistema;
	}

	public void setTransferenciaCreditoSistema(Double transferenciaCreditoSistema) {
		this.transferenciaCreditoSistema = transferenciaCreditoSistema;
	}
	
}
