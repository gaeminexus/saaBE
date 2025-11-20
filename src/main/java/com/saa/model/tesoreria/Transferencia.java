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
 * <p>Pojo mapeo de tabla TSR.TRNS.
 * Entity Transferencias.
 * Almacena las transferencias realizadas entre las cuentas bancarias de una misma empresa.
 * Cada movimiento genera un asiento contable y afecta la conciliacion.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TRNS", schema = "TSR")
@SequenceGenerator(name = "SQ_TRNSCDGO", sequenceName = "TSR.SQ_TRNSCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TransferenciaAll", query = "select e from Transferencia e"),
	@NamedQuery(name = "TransferenciaId", query = "select e from Transferencia e where e.codigo = :id")
})
public class Transferencia implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "TRNSCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TRNSCDGO")
	private Long codigo;
	
	/**
	 * Fecha en la que se realiza la transferencia.
	 */
	@Basic
	@Column(name = "TRNSFCHA")
	private LocalDateTime fecha;
	
	/**
	 * Tipo. 1 = Apertura de cuenta, 0 = Transferencia normal despues de apertura.
	 */
	@Basic
	@Column(name = "TRNSTPOO")
	private Long tipo;
	
	/**
	 * Banco origen. Desde el que se obtiene el dinero.
	 */
	@ManyToOne
	@JoinColumn(name = "BNCOORGN", referencedColumnName = "BNCOCDGO")
	private Banco bancoOrigen;	

	/**
	 * Cuenta bancaria origen. Desde la que se obtiene el dinero.
	 */
	@ManyToOne
	@JoinColumn(name = "CNBCORGN", referencedColumnName = "CNBCCDGO")
	private CuentaBancaria cuentaBancariaOrigen;	

	/**
	 * Numero de la cuenta origen.
	 */
	@Basic
	@Column(name = "CNTAORGN", length = 50)
	private String numeroCuentaOrigen;
	
	/**
	 * Banco destino. Al que se deposita el dinero.
	 */
	@ManyToOne
	@JoinColumn(name = "BNCODSTN", referencedColumnName = "BNCOCDGO")
	private Banco bancoDestino;	

	/**
	 * Cuenta bancaria destino. A la que se deposita el dinero.
	 */
	@ManyToOne
	@JoinColumn(name = "CNBCDSTN", referencedColumnName = "CNBCCDGO")
	private CuentaBancaria cuentaBancariaDestino;	

	/**
	 * ANumero de la cuenta destino.
	 */
	@Basic
	@Column(name = "CNTADSTN", length = 50)
	private String numeroCuentaDestino;
	
	/**
	 * Valor de la transferencia.
	 */
	@Basic
	@Column(name = "TRNSVLRR")
	private Double valor;
	
	/**
	 * Nombre del usuario que realiza la transferencia.
	 */
	@Basic
	@Column(name = "TRNSUSRO", length = 50)
	private String nombreUsuario;
	
	/**
	 * Para incluir observaciones a la transferencia.
	 */
	@Basic
	@Column(name = "TRNSOBSR", length = 300)
	private String observacion;
	
	/**
	 * Estado. 1 = Activo, 2 = Inactivo.
	 */
	@Basic
	@Column(name = "TRNSESTD")
	private Long estado;	
	
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
	 * Devuelve tipo
	 * @return tipo
	 */
	public Long getTipo() {
		return tipo;
	}

	/**
	 * Asigna tipo
	 * @param tipo Nuevo valor para tipo 
	 */
	public void setTipo(Long tipo) {
		this.tipo = tipo;
	}
	
	/**
	 * Devuelve banco
	 */
	public Banco getBancoOrigen() {
		return this.bancoOrigen;
	}
	
	/**
	 * Asigna banco
	 */
	public void setBancoOrigen(Banco bancoOrigen) {
		this.bancoOrigen = bancoOrigen;
	}

	/**
	 * Devuelve cuentaBancaria
	 */
	public CuentaBancaria getCuentaBancariaOrigen() {
		return this.cuentaBancariaOrigen;
	}
	
	/**
	 * Asigna cuentaBancaria
	 */
	public void setCuentaBancariaOrigen(CuentaBancaria cuentaBancariaOrigen) {
		this.cuentaBancariaOrigen = cuentaBancariaOrigen;
	}

	/**
	 * Devuelve numeroCuentaOrigen
	 * @return numeroCuentaOrigen
	 */
	public String getNumeroCuentaOrigen() {
		return numeroCuentaOrigen;
	}

	/**
	 * Asigna numeroCuentaOrigen
	 * @param numeroCuentaOrigen Nuevo valor para numeroCuentaOrigen 
	 */
	public void setNumeroCuentaOrigen(String numeroCuentaOrigen) {
		this.numeroCuentaOrigen = numeroCuentaOrigen;
	}
	
	/**
	 * Devuelve bancoDestino
	 */
	public Banco getBancoDestino() {
		return this.bancoDestino;
	}
	
	/**
	 * Asigna bancoDestino
	 */
	public void setBancoDestino(Banco bancoDestino) {
		this.bancoDestino = bancoDestino;
	}

	/**
	 * Devuelve cuentaBancariaDestino
	 */
	public CuentaBancaria getCuentaBancariaDestino() {
		return this.cuentaBancariaDestino;
	}
	
	/**
	 * Asigna cuentaBancariaDestino
	 */
	public void setCuentaBancariaDestino(CuentaBancaria cuentaBancariaDestino) {
		this.cuentaBancariaDestino = cuentaBancariaDestino;
	}

	/**
	 * Devuelve
	 * @return numeroCuentaDestino
	 */
	public String getNumeroCuentaDestino() {
		return numeroCuentaDestino;
	}

	/**
	 * Asigna numeroCuentaDestino
	 * @param numeroCuentaDestino Nuevo valor para numeroCuentaDestino 
	 */
	public void setNumeroCuentaDestino(String numeroCuentaDestino) {
		this.numeroCuentaDestino = numeroCuentaDestino;
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
	
	/**
	 * Devuelve nombreUsuario
	 * @return nombreUsuario
	 */
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	/**
	 * Asigna nombreUsuario
	 * @param nombreUsuario Nuevo valor para nombreUsuario 
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}
	
	/**
	 * Devuelve observacion
	 * @return observacion
	 */
	public String getObservacion() {
		return observacion;
	}

	/**
	 * Asigna observacion
	 * @param observacion Nuevo valor para observacion 
	 */
	public void setObservacion(String observacion) {
		this.observacion = observacion;
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
}
