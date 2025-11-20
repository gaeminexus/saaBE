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
 *  @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.CCXC. 
 *  Entity CajaLogicaPorCajaFisica. 
 *  Cajas logicas asignadas a una caja fisica.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CCXC", schema = "TSR")
@SequenceGenerator(name = "SQ_CCXCCDGO", sequenceName = "TSR.SQ_CCXCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "CajaLogicaPorCajaFisicaAll", query = "select e from CajaLogicaPorCajaFisica e"),
	@NamedQuery(name = "CajaLogicaPorCajaFisicaId", query = "select e from CajaLogicaPorCajaFisica e where e.codigo = :id")
})
public class CajaLogicaPorCajaFisica implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "CCXCCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CCXCCDGO")
	private Long codigo;
	
	/**
	 * Cajas Logicas asignadas a una caja fisica
	 */
	@ManyToOne
	@JoinColumn(name = "CJCNCDGO", referencedColumnName = "CJCNCDGO")
	private CajaLogica cajaLogica;	

	/**
	 * Caja fisica que contiene una caja logica
	 */
	@ManyToOne
	@JoinColumn(name = "CJAACDGO", referencedColumnName = "CJAACDGO")
	private CajaFisica cajaFisica;	

	/**
	 * Estado.
	 */
	@Basic
	@Column(name = "CCXCESTD")
	private Long estado;
	
	/**
	 * Fecha de ingreso al sistema.
	 */
	@Basic
	@Column(name = "CCXCFCIN")
	private LocalDateTime fechaIngreso;	
	
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
	 * Devuelve cajaLogica
	 */
	public CajaLogica getCajaLogica() {
		return this.cajaLogica;
	}
	
	/**
	 * Asigna cajaLogica
	 */
	public void setCajaLogica(CajaLogica cajaLogica) {
		this.cajaLogica = cajaLogica;
	}

	/**
	 * Devuelve cajaFisica
	 */
	public CajaFisica getCajaFisica() {
		return this.cajaFisica;
	}
	
	/**
	 * Asigna cajaFisica
	 */
	public void setCajaFisica(CajaFisica cajaFisica) {
		this.cajaFisica = cajaFisica;
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
	 * Devuelve fechaIngreso
	 * @return fechaIngreso
	 */
	public LocalDateTime getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna fechaIngreso
	 * @param fechaIngreso Nuevo valor para fechaIngreso 
	 */
	public void setFechaIngreso(LocalDateTime fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

}
