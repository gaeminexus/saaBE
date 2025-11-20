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
 *  @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.USXC. 
 *  Entity UsuarioPorCaja.
 *  Registra los usuarios asignados a cada caja fisica.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "USXC", schema = "TSR")
@SequenceGenerator(name = "SQ_USXCCDGO", sequenceName = "TSR.SQ_USXCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "UsuarioPorCajaAll", query = "select e from UsuarioPorCaja e"),
	@NamedQuery(name = "UsuarioPorCajaId", query = "select e from UsuarioPorCaja e where e.codigo = :id")
})
public class UsuarioPorCaja implements Serializable {

	/**
	 * Id.
	 */
	@Basic
	@Id
	@Column(name = "USXCCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_USXCCDGO")
	private Long codigo;
	
	/**
	 * Caja fisica a la que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "CJAACDGO", referencedColumnName = "CJAACDGO")
	private CajaFisica cajaFisica;	

	/**
	 * Nombre.
	 */
	@Basic
	@Column(name = "USXCNMBR", length = 500)
	private String nombre;
	
	/**
	 * Usuario a la que pertenece el usuario por caja.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDUS", referencedColumnName = "PJRQCDGO")
	private Usuario usuario;

	/**
	 * Fecha de ingreso al sistema.
	 */
	@Basic
	@Column(name = "USXCFCIN")
	private LocalDateTime fechaIngreso;
	
	/**
	 * Fecha de desactivacion.
	 */
	@Basic
	@Column(name = "USXCFCDS")
	private LocalDateTime fechaInactivo;
	
	/**
	 * Attribute estado.
	 */
	@Basic
	@Column(name = "USXCESTD")
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
