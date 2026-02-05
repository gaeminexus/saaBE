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
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;

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
 *  @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.CRCJ.
 *  Entity Cierre de caja.
 *  Almacena los cierres de caja realizados por un usuario por caja.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRCJ", schema = "TSR")
@SequenceGenerator(name = "SQ_CRCJCDGO", sequenceName = "TSR.SQ_CRCJCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "CierreCajaAll", query = "select e from CierreCaja e"),
	@NamedQuery(name = "CierreCajaId", query = "select e from CierreCaja e where e.codigo = :id")
})
public class CierreCaja implements Serializable {

	/**
	 * Id.
	 */
	@Id
	@Basic
	@Column(name = "CRCJCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CRCJCDGO")
	private Long codigo;
	
	/**
	 * Usuario por caja al que pertenece el cierre de caja
	 */
	@ManyToOne
	@JoinColumn(name = "USXCCDGO", referencedColumnName = "USXCCDGO")
	private UsuarioPorCaja usuarioPorCaja;	

	/**
	 * Fecha de cierre.
	 */
	@Basic
	@Column(name = "CRCJFCHA")
	private LocalDateTime fechaCierre;
	
	/**
	 * Nombre del usuario que realiza el cierre.
	 */
	@Basic
	@Column(name = "CRCJUSRO", length = 50)
	private String nombreUsuario;
	
	/**
	 * Monto total del cierre.
	 */
	@Basic
	@Column(name = "CRCJMNTO")
	private Double monto;
	
	/**
	 * Rubro 32. Estado del cierre.
	 */
	@Basic
	@Column(name = "CRCJRYYA")
	private Long rubroEstadoP;
	
	/**
	 * Detalle de Rubro 32. Estado del cierre.
	 */
	@Basic
	@Column(name = "CRCJRZZA")
	private Long rubroEstadoH;
	
	/**
	 * Monto total de efectivo en el cierre.
	 */
	@Basic
	@Column(name = "CRCJMNEF")
	private Double montoEfectivo;
	
	/**
	 * Monto total de cheque en el cierre.
	 */
	@Basic
	@Column(name = "CRCJMNCH")
	private Double montoCheque;
	
	/**
	 * Monto total de tarjeta en el cierre.
	 */
	@Basic
	@Column(name = "CRCJMNTJ")
	private Double montoTarjeta;
	
	/**
	 * Monto total de transferencias en el cierre.
	 */
	@Basic
	@Column(name = "CRCJMNTR")
	private Double montoTransferencia;
	
	/**
	 * Monto total de retencion en el cierre.
	 */
	@Basic
	@Column(name = "CRCJMNRT")
	private Double montoRetencion;
	
	/**
	 * Deposito en el que esta incluido el cierre
	 */
	@ManyToOne
	@JoinColumn(name = "DPSTCDGO", referencedColumnName = "DPSTCDGO")
	private Deposito deposito;

	/**
	 * Asiento contable ligado a la emision del cheque
	 */
	@ManyToOne
	@JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
	private Asiento asiento;
	
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
	 * Devuelve usuarioPorCaja
	 */
	public UsuarioPorCaja getUsuarioPorCaja() {
		return this.usuarioPorCaja;
	}
	
	/**
	 * Asigna usuarioPorCaja
	 */
	public void setUsuarioPorCaja(UsuarioPorCaja usuarioPorCaja) {
		this.usuarioPorCaja = usuarioPorCaja;
	}

	/**
	 * Devuelve fechaCierre
	 * @return fechaCierre
	 */
	public LocalDateTime getFechaCierre() {
		return fechaCierre;
	}

	/**
	 * Asigna fechaCierre
	 * @param fechaCierre Nuevo valor para fechaCierre 
	 */
	public void setFechaCierre(LocalDateTime fechaCierre) {
		this.fechaCierre = fechaCierre;
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
	 * Devuelve monto
	 * @return monto
	 */
	public Double getMonto() {
		return monto;
	}

	/**
	 * Asigna monto
	 * @param monto Nuevo valor para monto 
	 */
	public void setMonto(Double monto) {
		this.monto = monto;
	}
	
	/**
	 * Devuelve rubroEstadoP
	 * @return rubroEstadoP
	 */
	public Long getRubroEstadoP() {
		return rubroEstadoP;
	}

	/**
	 * Asigna rubroEstadoP
	 * @param rubroEstadoP Nuevo valor para rubroEstadoP 
	 */
	public void setRubroEstadoP(Long rubroEstadoP) {
		this.rubroEstadoP = rubroEstadoP;
	}
	
	/**
	 * Devuelve rubroEstadoH
	 * @return rubroEstadoH
	 */
	public Long getRubroEstadoH() {
		return rubroEstadoH;
	}

	/**
	 * Asigna rubroEstadoH
	 * @param rubroEstadoH Nuevo valor para rubroEstadoH 
	 */
	public void setRubroEstadoH(Long rubroEstadoH) {
		this.rubroEstadoH = rubroEstadoH;
	}
	
	/**
	 * Devuelve montoEfectivo
	 * @return montoEfectivo
	 */
	public Double getMontoEfectivo() {
		return montoEfectivo;
	}

	/**
	 * Asigna montoEfectivo
	 * @param montoEfectivo Nuevo valor para montoEfectivo 
	 */
	public void setMontoEfectivo(Double montoEfectivo) {
		this.montoEfectivo = montoEfectivo;
	}
	
	/**
	 * Devuelve montoCheque
	 * @return montoCheque
	 */
	public Double getMontoCheque() {
		return montoCheque;
	}

	/**
	 * Asigna montoCheque
	 * @param montoCheque Nuevo valor para montoCheque 
	 */
	public void setMontoCheque(Double montoCheque) {
		this.montoCheque = montoCheque;
	}
	
	/**
	 * Devuelve montoTarjeta
	 * @return montoTarjeta
	 */
	public Double getMontoTarjeta() {
		return montoTarjeta;
	}

	/**
	 * Asigna montoTarjeta
	 * @param montoTarjeta Nuevo valor para montoTarjeta 
	 */
	public void setMontoTarjeta(Double montoTarjeta) {
		this.montoTarjeta = montoTarjeta;
	}
	
	/**
	 * Devuelve montoTransferencia
	 * @return montoTransferencia
	 */
	public Double getMontoTransferencia() {
		return montoTransferencia;
	}

	/**
	 * Asigna montoTransferencia
	 * @param montoTransferencia Nuevo valor para montoTransferencia 
	 */
	public void setMontoTransferencia(Double montoTransferencia) {
		this.montoTransferencia = montoTransferencia;
	}
	
	/**
	 * Devuelve montoRetencion
	 * @return montoRetencion
	 */
	public Double getMontoRetencion() {
		return montoRetencion;
	}

	/**
	 * Asigna montoRetencion
	 * @param montoRetencion Nuevo valor para montoRetencion 
	 */
	public void setMontoRetencion(Double montoRetencion) {
		this.montoRetencion = montoRetencion;
	}
	
	/**
	 * Devuelve Deposito
	 */
	public Deposito getDeposito() {
		return deposito;
	}

	/**
	 * Asigna deposito
	 */
	public void setDeposito(Deposito deposito) {
		this.deposito = deposito;
	}

	/**
	 * Devuelve asiento
	 */
	public Asiento getAsiento() {
		return asiento;
	}

	/**
	 * Asigna asiento
	 */
	public void setAsiento(Asiento asiento) {
		this.asiento = asiento;
	}
	
}
