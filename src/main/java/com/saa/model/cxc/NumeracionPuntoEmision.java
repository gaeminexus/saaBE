package com.saa.model.cxc;

import java.io.Serializable;

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
 * Entidad que representa la Numeración por Punto de Emisión
 * Tabla: CBR.NXPE
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NXPE", schema = "CBR")
@NamedQueries({
	@NamedQuery(name = "NumeracionPuntoEmisionAll", query = "select e from NumeracionPuntoEmision e"),
	@NamedQuery(name = "NumeracionPuntoEmisionId", query = "select e from NumeracionPuntoEmision e where e.id = :id")
})
public class NumeracionPuntoEmision implements Serializable {

	/**
	 * ID de la numeración
	 */
	@Basic
	@Id
	@Column(name = "ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Punto de emisión al que pertenece
	 */
	@ManyToOne
	@JoinColumn(name = "PTOEMISION", referencedColumnName = "ID")
	private PuntoEmision ptoEmision;
	
	/**
	 * Tipo de documento
	 */
	@Basic
	@Column(name = "TIPODOC", length = 10)
	private String tipoDoc;
	
	/**
	 * Número actual
	 */
	@Basic
	@Column(name = "NUMACTUAL")
	private Long numActual;

	// Getters y Setters
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PuntoEmision getPtoEmision() {
		return ptoEmision;
	}

	public void setPtoEmision(PuntoEmision ptoEmision) {
		this.ptoEmision = ptoEmision;
	}

	public String getTipoDoc() {
		return tipoDoc;
	}

	public void setTipoDoc(String tipoDoc) {
		this.tipoDoc = tipoDoc;
	}

	public Long getNumActual() {
		return numActual;
	}

	public void setNumActual(Long numActual) {
		this.numActual = numActual;
	}
}
