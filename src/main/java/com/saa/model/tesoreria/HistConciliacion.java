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
 * <p>Pojo mapeo de tabla TSR.CNCH.
 * Entity Conciliacion Historica.
 * Almacena la cabecera de la conciliacion historica.
 * Cada vez que se desconcilia se almacena un registro histórico.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNCH", schema = "TSR")
@SequenceGenerator(name = "SQ_CNCHCDGO", sequenceName = "TSR.SQ_CNCHCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "HistConciliacionAll", query = "select e from HistConciliacion e"),
	@NamedQuery(name = "HistConciliacionId", query = "select e from HistConciliacion e where e.codigo = :id")
})
public class HistConciliacion implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "CNCHCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CNCHCDGO")
	private Long codigo;
	
	/**
	 * Id del periodo contable en el que se realizo la conciliacion.
	 */
	@Basic
	@Column(name = "CNCHPRDO")
	private Long idPeriodo;
	
	/**
	 * Usuario que realiza la conciliacion.
	 */
	@ManyToOne
	@JoinColumn(name = "CNCHUSRR", referencedColumnName = "PJRQCDGO")
	private Usuario usuario;
	
	/**
	 * Fecha en que se realiza la conciliacion.
	 */
	@Basic
	@Column(name = "CNCHFCHA")
	private LocalDateTime fecha;
	
	/**
	 * Estado de la conciliacion historica.
	 */
	@Basic
	@Column(name = "CNCHESTD")
	private Long estado;
	
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
	@Column(name = "CNCHSILB")
	private Double inicialSistema;
	
	/**
	 * Total en depositos de la cuenta bancaria en periodo segun el sistema.
	 */
	@Basic
	@Column(name = "CNCHDPST")
	private Double depositoSistema;
	
	/**
	 * Total en notas de credito de la cuenta bancaria en periodo segun el sistema.
	 */
	@Basic
	@Column(name = "CNCHNCRD")
	private Double creditoSistema;
	
	/**
	 * Total en cheques emitidos de la cuenta bancaria en periodo segun el sistema.
	 */
	@Basic
	@Column(name = "CNCHCHQE")
	private Double chequeSistema;
	
	/**
	 * Total en notas de debito de la cuenta bancaria en periodo segun el sistema.
	 */
	@Basic
	@Column(name = "CNCHNTDB")
	private Double debitoSistema;
	
	/**
	 * Saldo final segun los registros contables del sistema.
	 */
	@Basic
	@Column(name = "CNCHSLDF")
	private Double finalSistema;
	
	/**
	 * Saldo Final segun el estado de cuenta del banco.
	 */
	@Basic
	@Column(name = "CNCHSLDE")
	private Double saldoEstadoCuenta;
	
	/**
	 * Total en depositos en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCHDPTR")
	private Double depositoTransito;
	
	/**
	 * Total en cheques en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCHCHNC")
	private Double chequeTransito;
	
	/**
	 * Total en notas de credito en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCHNCTR")
	private Double creditoTransito;
	
	/**
	 * Total en notas de debito en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCHNDTR")
	private Double debitoTransito;
	
	/**
	 * Saldo final del banco luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCHSLDB")
	private Double saldoBanco;
	
	/**
	 * Empresa en la que se realiza la conciliacion.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;		
	
	/**
	 * Total en transferencias de debito en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCHTDTR")
	private Double transferenciaDebitoTransito;
	
	/**
	 * Total en transferencias de credito en transito de la cuenta bancaria luego de la conciliacion.
	 */
	@Basic
	@Column(name = "CNCHTCTR")
	private Double transferenciaCreditoTransito;
	
	/**
	 * Total en transferencias de debito de la cuenta bancaria en periodo segun sistema.
	 */
	@Basic
	@Column(name = "CNCHTRDB")
	private Double transferenciaDebitoSistema;
	
	/**
	 * Total en transferencias de credito de la cuenta bancaria en periodo segun sistema.
	 */
	@Basic
	@Column(name = "CNCHTRCR")
	private Double transferenciaCreditoSistema;
	
	/**
	 * Id de la conciliacion que origina el respaldo
	 */
	@Basic
	@Column(name = "CNCHCNCD")
	private Long idConciliacionOrigen;
	
	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo Nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve idPeriodo
	 * @return idPeriodo
	 */
	public Long getIdPeriodo() {
		return idPeriodo;
	}

	/**
	 * Asigna idPeriodo
	 * @param idPeriodo Nuevo valor para idPeriodo 
	 */
	public void setIdPeriodo(Long idPeriodo) {
		this.idPeriodo = idPeriodo;
	}
	
	/**
	 * Devuelve usuario
	 * @return usuario
	 */
	public Usuario getUsuario() {
		return usuario;
	}

	/**
	 * Asigna usuario
	 * @param usuario Nuevo valor para usuario 
	 */
	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}
	
	/**
	 * Devuelve fecha
	 * @return fecha
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna fecha
	 * @param fecha Nuevo valor para fecha 
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
	
	/**
	 * Devuelve estado
	 * @return estado
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna estado
	 * @param estado Nuevo valor para estado
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve cuentaBancaria
	 */
	public CuentaBancaria getCuentaBancaria() {
		return this.cuentaBancaria;
	}
	
	/**
	 * Asigna cuentaBancaria
	 */
	public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
		this.cuentaBancaria = cuentaBancaria;
	}

	/**
	 * Devuelve inicialSistema
	 * @return inicialSistema
	 */
	public Double getInicialSistema() {
		return inicialSistema;
	}

	/**
	 * Asigna inicialSistema
	 * @param inicialSistema Nuevo valor para inicialSistema 
	 */
	public void setInicialSistema(Double inicialSistema) {
		this.inicialSistema = inicialSistema;
	}
	
	/**
	 * Devuelve depositoSistema
	 * @return depositoSistema
	 */
	public Double getDepositoSistema() {
		return depositoSistema;
	}

	/**
	 * Asigna depositoSistema
	 * @param depositoSistema Nuevo valor para depositoSistema 
	 */
	public void setDepositoSistema(Double depositoSistema) {
		this.depositoSistema = depositoSistema;
	}
	
	/**
	 * Devuelve creditoSistema
	 * @return creditoSistema
	 */
	public Double getCreditoSistema() {
		return creditoSistema;
	}

	/**
	 * Asigna creditoSistema
	 * @param creditoSistema Nuevo valor para creditoSistema 
	 */
	public void setCreditoSistema(Double creditoSistema) {
		this.creditoSistema = creditoSistema;
	}
	
	/**
	 * Devuelve chequeSistema
	 * @return chequeSistema
	 */
	public Double getChequeSistema() {
		return chequeSistema;
	}

	/**
	 * Asigna chequeSistema
	 * @param chequeSistema Nuevo valor para chequeSistema 
	 */
	public void setChequeSistema(Double chequeSistema) {
		this.chequeSistema = chequeSistema;
	}
	
	/**
	 * Devuelve debitoSistema
	 * @return debitoSistema
	 */
	public Double getDebitoSistema() {
		return debitoSistema;
	}

	/**
	 * Asigna debitoSistema
	 * @param debitoSistema Nuevo valor para debitoSistema 
	 */
	public void setDebitoSistema(Double debitoSistema) {
		this.debitoSistema = debitoSistema;
	}
	
	/**
	 * Devuelve finalSistema
	 * @return finalSistema
	 */
	public Double getFinalSistema() {
		return finalSistema;
	}

	/**
	 * Asigna finalSistema
	 * @param finalSistema Nuevo valor para finalSistema 
	 */
	public void setFinalSistema(Double finalSistema) {
		this.finalSistema = finalSistema;
	}
	
	/**
	 * Devuelve saldoEstadoCuenta
	 * @return saldoEstadoCuenta
	 */
	public Double getSaldoEstadoCuenta() {
		return saldoEstadoCuenta;
	}

	/**
	 * Asigna saldoEstadoCuenta
	 * @param saldoEstadoCuenta Nuevo valor para saldoEstadoCuenta 
	 */
	public void setSaldoEstadoCuenta(Double saldoEstadoCuenta) {
		this.saldoEstadoCuenta = saldoEstadoCuenta;
	}
	
	/**
	 * Devuelve depositoTransito
	 * @return depositoTransito
	 */
	public Double getDepositoTransito() {
		return depositoTransito;
	}

	/**
	 * Asigna depositoTransito
	 * @param depositoTransito Nuevo valor para depositoTransito 
	 */
	public void setDepositoTransito(Double depositoTransito) {
		this.depositoTransito = depositoTransito;
	}
	
	/**
	 * Devuelve chequeTransito
	 * @return chequeTransito
	 */
	public Double getChequeTransito() {
		return chequeTransito;
	}

	/**
	 * Asigna chequeTransito
	 * @param chequeTransito Nuevo valor para chequeTransito 
	 */
	public void setChequeTransito(Double chequeTransito) {
		this.chequeTransito = chequeTransito;
	}
	
	/**
	 * Devuelve creditoTransito
	 * @return creditoTransito
	 */
	public Double getCreditoTransito() {
		return creditoTransito;
	}

	/**
	 * Asigna creditoTransito
	 * @param creditoTransito Nuevo valor para creditoTransito 
	 */
	public void setCreditoTransito(Double creditoTransito) {
		this.creditoTransito = creditoTransito;
	}
	
	/**
	 * Devuelve debitoTransito
	 * @return debitoTransito
	 */
	public Double getDebitoTransito() {
		return debitoTransito;
	}

	/**
	 * Asigna debitoTransito
	 * @param debitoTransito Nuevo valor para debitoTransito 
	 */
	public void setDebitoTransito(Double debitoTransito) {
		this.debitoTransito = debitoTransito;
	}
	
	/**
	 * Devuelve saldoBanco
	 * @return saldoBanco
	 */
	public Double getSaldoBanco() {
		return saldoBanco;
	}

	/**
	 * Asigna saldoBanco
	 * @param saldoBanco Nuevo valor para saldoBanco 
	 */
	public void setSaldoBanco(Double saldoBanco) {
		this.saldoBanco = saldoBanco;
	}
	
	/**
	 * Devuelve empresa
	 */
	public Empresa getEmpresa() {
		return this.empresa;
	}
	
	/**
	 * Asigna empresa
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}	
	
	/**
	 * Devuelve transferenciaDebitoTransito
	 * @return transferenciaDebitoTransito
	 */
	public Double getTransferenciaDebitoTransito() {
		return transferenciaDebitoTransito;
	}

	/**
	 * Asigna transferenciaDebitoTransito
	 * @param transferenciaDebitoTransito Nuevo valor para transferenciaDebitoTransito 
	 */
	public void setTransferenciaDebitoTransito(Double transferenciaDebitoTransito) {
		this.transferenciaDebitoTransito = transferenciaDebitoTransito;
	}
	
	/**
	 * Devuelve transferenciaCreditoTransito
	 * @return transferenciaCreditoTransito
	 */
	public Double getTransferenciaCreditoTransito() {
		return transferenciaCreditoTransito;
	}

	/**
	 * Asigna transferenciaCreditoTransito
	 * @param transferenciaCreditoTransito Nuevo valor para transferenciaCreditoTransito 
	 */
	public void setTransferenciaCreditoTransito(Double transferenciaCreditoTransito) {
		this.transferenciaCreditoTransito = transferenciaCreditoTransito;
	}
	
	/**
	 * Devuelve transferenciaDebitoSistema
	 * @return transferenciaDebitoSistema
	 */
	public Double getTransferenciaDebitoSistema() {
		return transferenciaDebitoSistema;
	}

	/**
	 * Asigna transferenciaDebitoSistema
	 * @param transferenciaDebitoSistema Nuevo valor para transferenciaDebitoSistema 
	 */
	public void setTransferenciaDebitoSistema(Double transferenciaDebitoSistema) {
		this.transferenciaDebitoSistema = transferenciaDebitoSistema;
	}
	
	/**
	 * Devuelve transferenciaCreditoSistema
	 * @return transferenciaCreditoSistema
	 */
	public Double getTransferenciaCreditoSistema() {
		return transferenciaCreditoSistema;
	}

	/**
	 * Asigna transferenciaCreditoSistema
	 * @param transferenciaCreditoSistema Nuevo valor para transferenciaCreditoSistema 
	 */
	public void setTransferenciaCreditoSistema(Double transferenciaCreditoSistema) {
		this.transferenciaCreditoSistema = transferenciaCreditoSistema;
	}

	/**
	 * Devuelve idConciliacionOrigen
	 * @return idConciliacionOrigen
	 */
	public Long getIdConciliacionOrigen() {
		return idConciliacionOrigen;
	}

	/**
	 * Asigna idConciliacionOrigen
	 * @param idConciliacionOrigen Nuevo valor para idConciliacionOrigen 
	 */
	public void setIdConciliacionOrigen(Long idConciliacionOrigen) {
		this.idConciliacionOrigen = idConciliacionOrigen;
	}

}
