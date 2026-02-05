/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.model.tsr;

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
 * @author GaemiSoft 
 * <p>Pojo mapeo de tabla TSR.ADTD.
 * Entity Auxiliar para el proceso de deposito (Bancos).
 * Almacena de forma temporal los bancos a los que serán enviados el deposito.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ADTD", schema = "TSR")
@SequenceGenerator(name = "SQ_ADTDCDGO", sequenceName = "TSR.SQ_ADTDCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "AuxDepositoBancoAll", query = "select e from AuxDepositoBanco e"),
	@NamedQuery(name = "AuxDepositoBancoId", query = "select e from AuxDepositoBanco e where e.codigo = :id")
})
public class AuxDepositoBanco implements Serializable {

	/**
	 * Id.
	 */
	@Id
	@Basic
	@Column(name = "ADTDCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ADTDCDGO")
	private Long codigo;
	
	/**
	 * Banco al que se enviara el deposito
	 */
	@ManyToOne
	@JoinColumn(name = "BNCOCDGO", referencedColumnName = "BNCOCDGO")
	private Banco banco;	

	/**
	 * Cuenta bancaria a la que se enviara el deposito
	 */
	@ManyToOne
	@JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
	private CuentaBancaria cuentaBancaria;	

	/**
	 * Usuario por caja que realiza el deposito
	 */
	@ManyToOne
	@JoinColumn(name = "USXCCDGO", referencedColumnName = "USXCCDGO")
	private UsuarioPorCaja usuarioPorCaja;	

	/**
	 * Valor total a depositar en la cuenta.
	 */
	@Basic
	@Column(name = "ADTDVLRR")
	private Double valor;
	
	/**
	 * Valor efectivo a depositar en la cuenta.
	 */
	@Basic
	@Column(name = "ADTDVLEF")
	private Double valorEfectivo;
	
	/**
	 * Valor cheque a depositar en la cuenta.
	 */
	@Basic
	@Column(name = "ADTDVLCH")
	private Double valorCheque;	
	
	public Long getCodigo() {
		return codigo;
	}

	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	public Banco getBanco() {
		return this.banco;
	}
	
	public void setBanco(Banco banco) {
		this.banco = banco;
	}

	public CuentaBancaria getCuentaBancaria() {
		return this.cuentaBancaria;
	}
	
	public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
		this.cuentaBancaria = cuentaBancaria;
	}

	public UsuarioPorCaja getUsuarioPorCaja() {
		return this.usuarioPorCaja;
	}
	
	public void setUsuarioPorCaja(UsuarioPorCaja usuarioPorCaja) {
		this.usuarioPorCaja = usuarioPorCaja;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}
	
	public Double getValorEfectivo() {
		return valorEfectivo;
	}

	public void setValorEfectivo(Double valorEfectivo) {
		this.valorEfectivo = valorEfectivo;
	}
	
	public Double getValorCheque() {
		return valorCheque;
	}

	public void setValorCheque(Double valorCheque) {
		this.valorCheque = valorCheque;
	}
}
