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
 * <p>Pojo mapeo de tabla TSR.TCTR.
 *  Entity Cobro con transferencia.
 *  Almacena los cobros con transferencia.
 *  Es detalle de la entidad cobro.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CTRN", schema = "TSR")
@SequenceGenerator(name = "SQ_CTRNCDGO", sequenceName = "TSR.SQ_CTRNCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "CobroTransferenciaAll", query = "select e from CobroTransferencia e"),
	@NamedQuery(name = "CobroTransferenciaId", query = "select e from CobroTransferencia e where e.codigo = :id")
})
public class CobroTransferencia implements Serializable {

	/**
	 * Id.
	 */
	@Id
	@Basic
	@Column(name = "CTRNCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CTRNCDGO")
	private Long codigo;
	
	/**
	 * Cobro al que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "CBROCDGO", referencedColumnName = "CBROCDGO")
	private Cobro cobro;	
	
	/**
	 * Banco externo al que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "BEXTCDGO", referencedColumnName = "BEXTCDGO")
	private BancoExterno bancoExterno;	
	
	/**
	 * Cuenta origen de la transferencia.
	 */
	@Basic
	@Column(name = "CTRNCTOR", length = 50)
	private String cuentaOrigen;
	
	/**
	 * Número de transferencia
	 */
	@Basic
	@Column(name = "CTRNNMRO")
	private Long numeroTransferencia;
	
	/**
	 * Banco de la empresa a la que se realiza la transferencia
	 */
	@ManyToOne
	@JoinColumn(name = "BNCOCDGO", referencedColumnName = "BNCOCDGO")
	private Banco banco;	

	/**
	 * Cuenta bancaria a la que se realiza la transferencia
	 */
	@ManyToOne
	@JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
	private CuentaBancaria cuentaBancaria;	

	/**
	 * Cuenta destino de la transferencia.
	 */
	@Basic
	@Column(name = "CTRNCTDS", length = 50)
	private String cuentaDestino;
	
	/**
	 * Valor de la transferencia.
	 */
	@Basic
	@Column(name = "CTRNVLRR")
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
	 * Devuelve Cobro
	 */
	public Cobro getCobro() {
		return this.cobro;
	}
	
	/**
	 * Asigna Cobro
	 */
	public void setCobro(Cobro cobro) {
		this.cobro = cobro;
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
	 * Devuelve cuentaOrigen
	 * @return cuentaOrigen
	 */
	public String getCuentaOrigen() {
		return cuentaOrigen;
	}

	/**
	 * Asigna cuentaOrigen
	 * @param cuentaOrigen Nuevo valor para cuentaOrigen 
	 */
	public void setCuentaOrigen(String cuentaOrigen) {
		this.cuentaOrigen = cuentaOrigen;
	}
	
	
	/**
	 * Devuelve numeroTransferencia
	 * @return numeroTransferencia
	 */
	public Long getNumeroTransferencia() {
		return numeroTransferencia;
	}

	/**
	 * Asigna numeroTransferencia
	 * @param numeroTransferencia: Nuevo valor para numeroTransferencia
	 */
	public void setNumeroTransferencia(Long numeroTransferencia) {
		this.numeroTransferencia = numeroTransferencia;
	}

	/**
	 * Devuelve banco
	 */
	public Banco getBanco() {
		return this.banco;
	}
	
	/**
	 * Asigna banco
	 */
	public void setBanco(Banco banco) {
		this.banco = banco;
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
	 * Devuelve cuentaDestino
	 * @return cuentaDestino
	 */
	public String getCuentaDestino() {
		return cuentaDestino;
	}

	/**
	 * Asigna cuentaDestino
	 * @param cuentaDestino Nuevo valor para cuentaDestino 
	 */
	public void setCuentaDestino(String cuentaDestino) {
		this.cuentaDestino = cuentaDestino;
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
