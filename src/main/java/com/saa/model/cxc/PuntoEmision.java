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
 * Entidad que representa un Punto de Emisión del Establecimiento
 * Tabla: CBR.PTEM
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PTEM", schema = "CBR")
@NamedQueries({
	@NamedQuery(name = "PuntoEmisionAll", query = "select e from PuntoEmision e"),
	@NamedQuery(name = "PuntoEmisionId", query = "select e from PuntoEmision e where e.id = :id")
})
public class PuntoEmision implements Serializable {

	/**
	 * ID del punto de emisión
	 */
	@Basic
	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Código del punto de emisión
	 */
	@Basic
	@Column(name = "CODIGO", length = 45)
	private String codigo;
	
	/**
	 * Establecimiento al que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "ESTABLECIMIENTO", referencedColumnName = "ID")
	private Establecimiento establecimiento;
	
	/**
	 * Nombre del punto de emisión
	 */
	@Basic
	@Column(name = "NOMBRE", length = 500)
	private String nombre;
	
	/**
	 * Fecha de creación
	 */
	@Basic
	@Column(name = "CREADO")
	private LocalDateTime creado;
	
	/**
	 * Observación
	 */
	@Basic
	@Column(name = "OBSERVACION", length = 2000)
	private String observacion;
	
	/**
	 * ID del Transportista que es el Punto de Venta (si empTransporte = 1)
	 */
	@Basic
	@Column(name = "TRANSPORTISTA")
	private Long transportista;
	
	/**
	 * Estado del punto de emisión
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

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public Establecimiento getEstablecimiento() {
		return establecimiento;
	}

	public void setEstablecimiento(Establecimiento establecimiento) {
		this.establecimiento = establecimiento;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public LocalDateTime getCreado() {
		return creado;
	}

	public void setCreado(LocalDateTime creado) {
		this.creado = creado;
	}

	public String getObservacion() {
		return observacion;
	}

	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}

	public Long getTransportista() {
		return transportista;
	}

	public void setTransportista(Long transportista) {
		this.transportista = transportista;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}
}
