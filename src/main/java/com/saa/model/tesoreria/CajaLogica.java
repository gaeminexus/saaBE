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

import com.saa.model.cnt.PlanCuenta;

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
 * <p>Pojo mapeo de tabla TSR.CJCN. 
 *  Entity CajaLogica.
 *  Cajas contables o logicas. Detalle de la entidad GrupoCaja.
 *  Registra las cajas lógicas que pueden ser asignadas a una caja física.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CJCN", schema = "TSR")
@SequenceGenerator(name = "SQ_CJCNCDGO", sequenceName = "TSR.SQ_CJCNCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "CajaLogicaAll", query = "select e from CajaLogica e"),
	@NamedQuery(name = "CajaLogicaId", query = "select e from CajaLogica e where e.codigo = :id")
})
public class CajaLogica implements Serializable {

	/**
	 * Id.
	 */
	@Id
	@Basic
	@Column(name = "CJCNCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CJCNCDGO")
	private Long codigo;
	
	/**
	 * Grupo de cajas al que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "CJINCDGO", referencedColumnName = "CJINCDGO")
	private GrupoCaja grupoCaja;	

	/**
	 * Nombre de la caja logica.
	 */
	@Basic
	@Column(name = "CJCNNMBR", length = 500)
	private String nombre;
	
	/**
	 * Cuenta contable relacionada a la cuenta bancaria
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
	private PlanCuenta planCuenta;
	
	/**
	 * Cuenta contable asignada a la caja.
	 */
	@Basic
	@Column(name = "CJCNCNCT", length = 100)
	private String cuentaContable;
	
	/**
	 * Fecha de ingreso al sistema.
	 */
	@Basic
	@Column(name = "CJCNFCIN")
	private LocalDateTime fechaIngreso;
	
	/**
	 * Fecha de desactivacion.
	 */
	@Basic
	@Column(name = "CJCNFCDS")
	private LocalDateTime fechaInactivo;
	
	/**
	 * Attribute estado.
	 */
	@Basic
	@Column(name = "CJCNESTD")
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
	 * Devuelve grupoCaja
	 */
	public GrupoCaja getGrupoCaja() {
		return this.grupoCaja;
	}
	
	/**
	 * Asigna grupoCaja
	 */
	public void setGrupoCaja(GrupoCaja grupoCaja) {
		this.grupoCaja = grupoCaja;
	}

	/**
	 * Devuelve nombre
	 * @return nombre
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna nombre
	 * @param nombre Nuevo valor para nombre 
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Devuelve PlanCuenta
	 */
	public PlanCuenta getPlanCuenta() {
		return planCuenta;
	}

	/**
	 * Asigna planCuenta
	 */
	public void setPlanCuenta(PlanCuenta planCuenta) {
		this.planCuenta = planCuenta;
	}

	/**
	 * Devuelve cuentaContable
	 * @return cuentaContable
	 */
	public String getCuentaContable() {
		return cuentaContable;
	}

	/**
	 * Asigna cuentaContable
	 * @param cuentaContable Nuevo valor para cuentaContable 
	 */
	public void setCuentaContable(String cuentaContable) {
		this.cuentaContable = cuentaContable;
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
	
	/**
	 * Devuelve fechaInactivo
	 * @return fechaInactivo
	 */
	public LocalDateTime getFechaInactivo() {
		return fechaInactivo;
	}

	/**
	 * Asigna fechaInactivo
	 * @param fechaInactivo Nuevo valor para fechaInactivo 
	 */
	public void setFechaInactivo(LocalDateTime fechaInactivo) {
		this.fechaInactivo = fechaInactivo;
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
