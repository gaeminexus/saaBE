package com.saa.model.contabilidad;

import java.io.Serializable;
import java.util.Date;

import com.saa.model.scp.Empresa;

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
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;


@SuppressWarnings("serial")
@Entity
@Table(name = "CNCS", schema = "CNT")
@SequenceGenerator(name = "SQ_CNCSCDGO", sequenceName = "CNT.SQ_CNCSCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "CentroCostoAll", query = "select e from CentroCosto e"),
	@NamedQuery(name = "CentroCostoId", query = "select e from CentroCosto e where e.codigo = :id")
})
public class CentroCosto implements Serializable {

	/**
	 * id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "CNCSCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_CNCSCDGO")
	private Long codigo;
	
	/**
	 * nombre de centro de costo.
	 */
	@Basic
	@Column(name = "CNCSNMBR", length = 100)
	private String nombre;
	
	/**
	 * número de centro de costo.
	 */
	@Basic
	@Column(name = "CNCSNMRO", length = 50)
	private String numero;
	
	/**
	 * tipo de centro de costo 1 = acumulacion, 2 = movimiento.
	 */
	@Basic
	@Column(name = "CNCSTPOO")
	private Long tipo;
	
	/**
	 * nivel del centro de costo.
	 */
	@Basic
	@Column(name = "CNCSNVLL")
	private Long nivel;
	
	/**
	 * código de padre.
	 */
	@Basic
	@Column(name = "CNCSCDPD")
	private Long idPadre;
	
	/**
	 * estado del centro de costo 1 = activo, 2 = inactivo.
	 */
	@Basic
	@Column(name = "CNCSESTD")
	private Long estado;
	
	/**
	 * Empresa.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;
	
	/**
	 * fecha desactivación centro de costo.
	 */
	@Basic
	@Column(name = "CNCSFCDS")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaInactivo;
	
	/**
	 * fecha de creación del centro de costo  fechaIngreso.
	 */
	@Basic
	@Column(name = "CNCSFCHA")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fechaIngreso;
	

	
	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}
	
	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve nombre
	 * @return nombre
	 */
	@Basic
	@Column(name = "CNCSNMBR", length = 100)
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna nombre
	 * @param nombre nuevo valor para nombre 
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	/**
	 * Devuelve numero
	 * @return numero
	 */
	public String getNumero() {
		return numero;
	}

	/**
	 * Asigna numero
	 * @param numero nuevo valor para numero 
	 */
	public void setNumero(String numero) {
		this.numero = numero;
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
	 * @param tipo nuevo valor para tipo 
	 */
	public void setTipo(Long tipo) {
		this.tipo = tipo;
	}
	
	/**
	 * Devuelve nivel
	 * @return nivel
	 */
	public Long getNivel() {
		return nivel;
	}

	/**
	 * Asigna nivel
	 * @param nivel nuevo valor para nivel 
	 */
	public void setNivel(Long nivel) {
		this.nivel = nivel;
	}
	
	/**
	 * Devuelve idPadre
	 * @return idPadre
	 */
	public Long getIdPadre() {
		return idPadre;
	}

	/**
	 * Asigna idPadre
	 * @param idPadre nuevo valor para idPadre 
	 */
	public void setIdPadre(Long idPadre) {
		this.idPadre = idPadre;
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
	 * @param estado nuevo valor para estado 
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}
	
	/**
	 * Devuelve empresa
	 */
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna empresa 
	 * @param empresa nuevo valor para empresa 
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
	
	/**
	 * Devuelve fechaInactivo
	 * @return fechaInactivo
	 */
	public Date getFechaInactivo() {
		return fechaInactivo;
	}

	/**
	 * Asigna fechaInactivo
	 * @param fechaInactivo nuevo valor para fechaInactivo 
	 */
	public void setFechaInactivo(Date fechaInactivo) {
		this.fechaInactivo = fechaInactivo;
	}
	
	/**
	 * Devuelve fechaIngreso
	 * @return fechaIngreso
	 */
	public Date getFechaIngreso() {
		return fechaIngreso;
	}

	/**
	 * Asigna fechaIngreso
	 * @param fechaIngreso nuevo valor para fechaIngreso 
	 */
	public void setFechaIngreso(Date fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}
	
	 
}
