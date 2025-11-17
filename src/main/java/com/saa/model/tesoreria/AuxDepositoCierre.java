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
 * <p>Pojo mapeo de tabla TSR.ACPD.
 * Entity Auxiliar para el proceso de deposito (Cierres).
 * Almacena de forma temporal los cierres para seleccionar los que serán enviados al deposito.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ACPD", schema = "TSR")
@SequenceGenerator(name = "SQ_ACPDCDGO", sequenceName = "TSR.SQ_ACPDCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "AuxDepositoCierreAll", query = "select e from AuxDepositoCierre e"),
	@NamedQuery(name = "AuxDepositoCierreId", query = "select e from AuxDepositoCierre e where e.codigo = :id")
})
public class AuxDepositoCierre implements Serializable {

    @Basic
    @Id
    @Column(name = "ACPDCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ACPDCDGO")
	private Long codigo;
	
    @ManyToOne
    @JoinColumn(name = "CRCJCDGO", referencedColumnName = "CRCJCDGO")
	private CierreCaja cierreCaja;	

    @ManyToOne
    @JoinColumn(name = "USXCCDGO", referencedColumnName = "USXCCDGO")
	private UsuarioPorCaja usuarioPorCaja;	

    @Basic
    @Column(name = "ACPDMNEF")
	private Double montoEfectivo;
	
    @Basic
    @Column(name = "ACPDMNCH")
	private Double montoCheque;
	
    @Basic
    @Column(name = "ACPDSLCC")
	private Long seleccionado;
	
    @Basic
    @Column(name = "ACPDMNDP")
	private Double montoDeposito;
	
    @Basic
    @Column(name = "ACPDMNTT")
	private Double montoTotalCierre;
	
    @Basic
    @Column(name = "ACPDFCCR")
	private LocalDateTime fechaCierre;
	
    @Basic
    @Column(name = "ACPDNMCJ", length = 500)
	private String nombreCaja;
	
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
	 * Devuelve cierreCaja
	 */
	public CierreCaja getCierreCaja() {
		return this.cierreCaja;
	}
	
	/**
	 * Asigna cierreCaja
	 */
	public void setCierreCaja(CierreCaja cierreCaja) {
		this.cierreCaja = cierreCaja;
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
	 * Devuelve seleccionado
	 * @return seleccionado
	 */
	public Long getSeleccionado() {
		return seleccionado;
	}

	/**
	 * Asigna seleccionado
	 * @param seleccionado Nuevo valor para seleccionado 
	 */
	public void setSeleccionado(Long seleccionado) {
		this.seleccionado = seleccionado;
	}
	
	/**
	 * Devuelve montoDeposito
	 * @return montoDeposito
	 */
	public Double getMontoDeposito() {
		return montoDeposito;
	}

	/**
	 * Asigna montoDeposito
	 * @param montoDeposito Nuevo valor para montoDeposito 
	 */
	public void setMontoDeposito(Double montoDeposito) {
		this.montoDeposito = montoDeposito;
	}
	
	/**
	 * Devuelve montoTotalCierre
	 * @return montoTotalCierre
	 */
	public Double getMontoTotalCierre() {
		return montoTotalCierre;
	}

	/**
	 * Asigna montoTotalCierre
	 * @param montoTotalCierre Nuevo valor para montoTotalCierre 
	 */
	public void setMontoTotalCierre(Double montoTotalCierre) {
		this.montoTotalCierre = montoTotalCierre;
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
	 * Devuelve nombreCaja
	 * @return nombreCaja
	 */
	public String getNombreCaja() {
		return nombreCaja;
	}

	/**
	 * Asigna nombreCaja
	 * @param nombreCaja Nuevo valor para nombreCaja 
	 */
	public void setNombreCaja(String nombreCaja) {
		this.nombreCaja = nombreCaja;
	}
	
}