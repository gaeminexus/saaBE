package com.saa.model.cxc;

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
import jakarta.persistence.Table;

/**
 * Entidad que representa un Establecimiento del Facturador
 * Tabla: CBR.ESTB
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ESTB", schema = "CBR")
@NamedQueries({
	@NamedQuery(name = "EstablecimientoAll", query = "select e from Establecimiento e"),
	@NamedQuery(name = "EstablecimientoId", query = "select e from Establecimiento e where e.id = :id")
})
public class Establecimiento implements Serializable {

	/**
	 * ID del establecimiento
	 */
	@Basic
	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Facturador al que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "FACTURADOR", referencedColumnName = "ID")
	private Facturador facturador;
	
	/**
	 * Código del establecimiento
	 */
	@Basic
	@Column(name = "CODIGO", length = 250)
	private String codigo;
	
	/**
	 * Nombre del establecimiento
	 */
	@Basic
	@Column(name = "NOMBRE", length = 250)
	private String nombre;
	
	/**
	 * Descripción
	 */
	@Basic
	@Column(name = "DESCRIPCION", length = 250)
	private String descripcion;
	
	/**
	 * Dirección
	 */
	@Basic
	@Column(name = "DIRECCION", length = 1000)
	private String direccion;
	
	/**
	 * Teléfono
	 */
	@Basic
	@Column(name = "TELEFONO", length = 1000)
	private String telefono;
	
	/**
	 * Email
	 */
	@Basic
	@Column(name = "MAIL", length = 45)
	private String mail;
	
	/**
	 * Logo
	 */
	@Basic
	@Column(name = "LOGO", length = 1000)
	private String logo;
	
	/**
	 * Fecha de creación
	 */
	@Basic
	@Column(name = "CREACION")
	private LocalDateTime creacion;
	
	/**
	 * Identifica si es el establecimiento matriz (1=Sí)
	 */
	@Basic
	@Column(name = "MATRIZ")
	private Long matriz;
	
	/**
	 * Estado del establecimiento
	 */
	@Basic
	@Column(name = "ESTADO")
	private Long estado;

	// Getters y Setters
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Facturador getFacturador() {
		return facturador;
	}

	public void setFacturador(Facturador facturador) {
		this.facturador = facturador;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	public LocalDateTime getCreacion() {
		return creacion;
	}

	public void setCreacion(LocalDateTime creacion) {
		this.creacion = creacion;
	}

	public Long getMatriz() {
		return matriz;
	}

	public void setMatriz(Long matriz) {
		this.matriz = matriz;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}
}
