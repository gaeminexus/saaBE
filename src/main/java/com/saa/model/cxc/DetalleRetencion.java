/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
package com.saa.model.cxc;

import java.io.Serializable;
import java.time.LocalDate;

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
 * Entity DetalleRetencion.
 * Almacena el detalle de retenciones.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTRT", schema = "CBR")
@NamedQueries({
	@NamedQuery(name = "DetalleRetencionAll", query = "select e from DetalleRetencion e"),
	@NamedQuery(name = "DetalleRetencionId", query = "select e from DetalleRetencion e where e.id = :id")
})
public class DetalleRetencion implements Serializable {

	@Basic
	@Id
	@Column(name = "ID", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "RETENCION", referencedColumnName = "ID")
	private Retencion retencion;
	
	@Basic
	@Column(name = "TIPODOCRETEN", length = 2)
	private String tipoDocReten;
	
	@Basic
	@Column(name = "NUMDOCRETEN", length = 100)
	private String numDocReten;
	
	@Basic
	@Column(name = "FECHAEMIDOC")
	private LocalDate fechaEmiDoc;
	
	@Basic
	@Column(name = "CODIMPUESTO", length = 2)
	private String codImpuesto;
	
	@Basic
	@Column(name = "CODRETENCION", length = 100)
	private String codRetencion;
	
	@Basic
	@Column(name = "BASEIMPONIBLE")
	private Double baseImponible;
	
	@Basic
	@Column(name = "PORCENTAJERETEN")
	private Double porcentajeReten;
	
	@Basic
	@Column(name = "VALORRETEN")
	private Double valorReten;
	
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

	public Retencion getRetencion() {
		return retencion;
	}

	public void setRetencion(Retencion retencion) {
		this.retencion = retencion;
	}

	public String getTipoDocReten() {
		return tipoDocReten;
	}

	public void setTipoDocReten(String tipoDocReten) {
		this.tipoDocReten = tipoDocReten;
	}

	public String getNumDocReten() {
		return numDocReten;
	}

	public void setNumDocReten(String numDocReten) {
		this.numDocReten = numDocReten;
	}

	public LocalDate getFechaEmiDoc() {
		return fechaEmiDoc;
	}

	public void setFechaEmiDoc(LocalDate fechaEmiDoc) {
		this.fechaEmiDoc = fechaEmiDoc;
	}

	public String getCodImpuesto() {
		return codImpuesto;
	}

	public void setCodImpuesto(String codImpuesto) {
		this.codImpuesto = codImpuesto;
	}

	public String getCodRetencion() {
		return codRetencion;
	}

	public void setCodRetencion(String codRetencion) {
		this.codRetencion = codRetencion;
	}

	public Double getBaseImponible() {
		return baseImponible;
	}

	public void setBaseImponible(Double baseImponible) {
		this.baseImponible = baseImponible;
	}

	public Double getPorcentajeReten() {
		return porcentajeReten;
	}

	public void setPorcentajeReten(Double porcentajeReten) {
		this.porcentajeReten = porcentajeReten;
	}

	public Double getValorReten() {
		return valorReten;
	}

	public void setValorReten(Double valorReten) {
		this.valorReten = valorReten;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}
}
