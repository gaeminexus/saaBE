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
 * <p>Pojo mapeo de tabla TSR.TCCH.
 *  Entity Temporal de cobro con cheque.
 *  Almacena el cobro con cheque de manera temporal.
 *  Luego pasara a la tabla TSR.CCHQ.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TCCH", schema = "TSR")
@SequenceGenerator(name = "SQ_TCCHCDGO", sequenceName = "TSR.SQ_TCCHCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempCobroChequeAll", query = "select e from TempCobroCheque e"),
	@NamedQuery(name = "TempCobroChequeId", query = "select e from TempCobroCheque e where e.codigo = :id")
})
public class TempCobroCheque implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "TCCHCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TCCHCDGO")
	private Long codigo;
	
	/**
	 * Cobro temporal al que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "TCBRCDGO", referencedColumnName = "TCBRCDGO")
	private TempCobro tempCobro;	

	/**
	 * Banco Externo del que se obtiene el cheque
	 */
	@ManyToOne
	@JoinColumn(name = "BEXTCDGO", referencedColumnName = "BEXTCDGO")
	private BancoExterno bancoExterno;	

	/**
	 * Numero de cheque.
	 */
	@Basic
	@Column(name = "TCCHNMRO")
	private Long numero;
	
	/**
	 * Valor del cheque.
	 */
	@Basic
	@Column(name = "TCCHVLRR")
	private Double valor;
	
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
	 * Devuelve tempCobro
	 */
	public TempCobro getTempCobro() {
		return this.tempCobro;
	}
	
	/**
	 * Asigna tempCobro
	 */
	public void setTempCobro(TempCobro tempCobro) {
		this.tempCobro = tempCobro;
	}

	/**
	 * Devuelve bancoExterno
	 */
	public BancoExterno getBancoExterno() {
		return this.bancoExterno;
	}
	
	/**
	 * Asigna bancoExterno
	 */
	public void setBancoExterno(BancoExterno bancoExterno) {
		this.bancoExterno = bancoExterno;
	}

	/**
	 * Devuelve numero
	 * @return numero
	 */
	public Long getNumero() {
		return numero;
	}

	/**
	 * Asigna numero
	 * @param numero Nuevo valor para numero 
	 */
	public void setNumero(Long numero) {
		this.numero = numero;
	}
	
	/**
	 * Devuelve valor
	 * @return valor
	 */
	public Double getValor() {
		return valor;
	}

	/**
	 * Asigna valor
	 * @param valor Nuevo valor para valor 
	 */
	public void setValor(Double valor) {
		this.valor = valor;
	}
}
