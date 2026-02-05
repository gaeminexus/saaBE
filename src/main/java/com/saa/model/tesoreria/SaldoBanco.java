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

import com.saa.model.cnt.Periodo;

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
 * <p>Pojo mapeo de tabla TSR.SLCB.
 * Entity Saldo cuenta bancaria.
 * Almacena los saldos por periodo de cada cuenta bancaria de la empresa.
 * Estos saldos se calculan el momento de la mayorizacion.
 * En caso de una desmayorizacion estos saldos se eliminan.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "SLCB", schema = "TSR")
@SequenceGenerator(name = "SQ_SLCBCDGO", sequenceName = "TSR.SQ_SLCBCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "SaldoBancoAll", query = "select e from SaldoBanco e"),
	@NamedQuery(name = "SaldoBancoId", query = "select e from SaldoBanco e where e.codigo = :id")
})
public class SaldoBanco implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "SLCBCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_SLCBCDGO")
	private Long codigo;
	
	/**
	 * Cuenta bancaria a la que se calculan los saldos.
	 */
	@ManyToOne
	@JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
	private CuentaBancaria cuentaBancaria;	

	/**
	 * Periodo contable en el que se calcula los saldos.
	 */
	@ManyToOne
	@JoinColumn(name = "PRDOCDGO", referencedColumnName = "PRDOCDGO")
	private Periodo periodo;	

	/**
	 * Numero de mes en el que se calcula el saldo.
	 */
	@Basic
	@Column(name = "PRDOMSSS")
	private Long numeroMes;
	
	/**
	 * Numero de año en el que se calcula el saldo.
	 */
	@Basic
	@Column(name = "PRDOANNN")
	private Long numeroAnio;
	
	/**
	 * Saldo de la cuenta bancaria en el periodo anterior.
	 */
	@Basic
	@Column(name = "SLCBANTR")
	private Double saldoAnterior;
	
	/**
	 * Total egresado de la cuenta bancaria en el periodo.
	 */
	@Basic
	@Column(name = "SLCBDBTO")
	private Double valorEgreso;
	
	/**
	 * Total ingresado de la cuenta bancaria en el periodo.
	 */
	@Basic
	@Column(name = "SLCBCRDT")
	private Double valorIngreso;
	
	/**
	 * Total en notas de debito de la cuenta bancaria en el periodo.
	 */
	@Basic
	@Column(name = "SLCBNTDB")
	private Double valorND;
	
	/**
	 * Total en notas de credito de la cuenta bancaria en el periodo.
	 */
	@Basic
	@Column(name = "SLCBNTCR")
	private Double valorNC;
	
	/**
	 * Saldo final de la cuenta bancaria.
	 */
	@Basic
	@Column(name = "SLCBFNLL")
	private Double saldoFinal;
	
	/**
	 * Total en transferencias de debito de la cuenta bancaria.
	 */
	@Basic
	@Column(name = "SLCBTRDB")
	private Double valorTransferenciaD;
	
	/**
	 * Total en transferencias de credito de la cuenta bancaria.
	 */
	@Basic
	@Column(name = "SLCBTRCR")
	private Double valorTransferenciaC;
	
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
	 * Devuelve periodo
	 */
	public Periodo getPeriodo() {
		return this.periodo;
	}
	
	/**
	 * Asigna periodo
	 */
	public void setPeriodo(Periodo periodo) {
		this.periodo = periodo;
	}

	/**
	 * Devuelve numeroMes
	 * @return numeroMes
	 */
	public Long getNumeroMes() {
		return numeroMes;
	}

	/**
	 * Asigna numeroMes
	 * @param numeroMes Nuevo valor para numeroMes 
	 */
	public void setNumeroMes(Long numeroMes) {
		this.numeroMes = numeroMes;
	}
	
	/**
	 * Devuelve numeroAnio
	 * @return numeroAnio
	 */
	public Long getNumeroAnio() {
		return numeroAnio;
	}

	/**Asigna numeroAnio
	 * @param numeroAnio Nuevo valor para numeroAnio 
	 */
	public void setNumeroAnio(Long numeroAnio) {
		this.numeroAnio = numeroAnio;
	}
	
	/**
	 * Devuelve saldoAnterior
	 * @return saldoAnterior
	 */
	public Double getSaldoAnterior() {
		return saldoAnterior;
	}

	/**
	 * Asigna saldoAnterior
	 * @param saldoAnterior Nuevo valor para saldoAnterior 
	 */
	public void setSaldoAnterior(Double saldoAnterior) {
		this.saldoAnterior = saldoAnterior;
	}
	
	/**Devuelve valorEgreso
	 * @return valorEgreso
	 */
	public Double getValorEgreso() {
		return valorEgreso;
	}

	/**
	 * Asigna valorEgreso
	 * @param valorEgreso Nuevo valor para valorEgreso 
	 */
	public void setValorEgreso(Double valorEgreso) {
		this.valorEgreso = valorEgreso;
	}
	
	/**
	 * Devuelve valorIngreso
	 * @return valorIngreso
	 */
	public Double getValorIngreso() {
		return valorIngreso;
	}

	/**
	 * Asigna valorIngreso
	 * @param valorIngreso Nuevo valor para valorIngreso 
	 */
	public void setValorIngreso(Double valorIngreso) {
		this.valorIngreso = valorIngreso;
	}
	
	/**
	 * Devuelve valorND
	 * @return valorND
	 */
	public Double getValorND() {
		return valorND;
	}

	/**
	 * Asigna valorND
	 * @param valorND Nuevo valor para valorND 
	 */
	public void setValorND(Double valorND) {
		this.valorND = valorND;
	}
	
	/**
	 * Devuelve valorNC
	 * @return valorNC
	 */
	public Double getValorNC() {
		return valorNC;
	}

	/**
	 * Asigna valorNC
	 * @param valorNC Nuevo valor para valorNC 
	 */
	public void setValorNC(Double valorNC) {
		this.valorNC = valorNC;
	}
	
	/**
	 * Devuelve saldoFinal
	 * @return saldoFinal
	 */
	public Double getSaldoFinal() {
		return saldoFinal;
	}

	/**
	 * Asigna saldoFinal
	 * @param saldoFinal Nuevo valor para saldoFinal 
	 */
	public void setSaldoFinal(Double saldoFinal) {
		this.saldoFinal = saldoFinal;
	}
	
	/**
	 * Devuelve valorTransferenciaD
	 * @return valorTransferenciaD
	 */
	public Double getValorTransferenciaD() {
		return valorTransferenciaD;
	}

	/**
	 * Asigna valorTransferenciaD
	 * @param valorTransferenciaD Nuevo valor para valorTransferenciaD 
	 */
	public void setValorTransferenciaD(Double valorTransferenciaD) {
		this.valorTransferenciaD = valorTransferenciaD;
	}
	
	/**
	 * Devuelve valorTransferenciaC
	 * @return valorTransferenciaC
	 */
	public Double getValorTransferenciaC() {
		return valorTransferenciaC;
	}

	/**
	 * Asigna valorTransferenciaC
	 * @param valorTransferenciaC Nuevo valor para valorTransferenciaC 
	 */
	public void setValorTransferenciaC(Double valorTransferenciaC) {
		this.valorTransferenciaC = valorTransferenciaC;
	}
}
