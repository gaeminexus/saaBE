/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
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
 * Entity PathLiquidacionCompra.
 * Almacena las rutas de archivos relacionados a la liquidación.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PTLC", schema = "CBR")
@NamedQueries({
	@NamedQuery(name = "PathLiquidacionCompraAll", query = "select e from PathLiquidacionCompra e"),
	@NamedQuery(name = "PathLiquidacionCompraId", query = "select e from PathLiquidacionCompra e where e.id = :id")
})
public class PathLiquidacionCompra implements Serializable {

	@Basic
	@Id
	@Column(name = "ID", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "LIQUIDACION", referencedColumnName = "ID")
	private LiquidacionCompra liquidacion;
	
	@Basic
	@Column(name = "PATH", length = 1000)
	private String path;
	
	@Basic
	@Column(name = "ALTERNO")
	private Long alterno;

	// Getters y Setters
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LiquidacionCompra getLiquidacion() {
		return liquidacion;
	}

	public void setLiquidacion(LiquidacionCompra liquidacion) {
		this.liquidacion = liquidacion;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Long getAlterno() {
		return alterno;
	}

	public void setAlterno(Long alterno) {
		this.alterno = alterno;
	}
}
