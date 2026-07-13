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
 * Entity PathRetencionV2.
 * Almacena las rutas de archivos relacionados a la retención V2.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRT2", schema = "CBR")
@NamedQueries({
	@NamedQuery(name = "PathRetencionV2All", query = "select e from PathRetencionV2 e"),
	@NamedQuery(name = "PathRetencionV2Id", query = "select e from PathRetencionV2 e where e.id = :id")
})
public class PathRetencionV2 implements Serializable {

	@Basic
	@Id
	@Column(name = "ID", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "RETENCIONV2", referencedColumnName = "ID")
	private RetencionV2 retencionV2;
	
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

	public RetencionV2 getRetencionV2() {
		return retencionV2;
	}

	public void setRetencionV2(RetencionV2 retencionV2) {
		this.retencionV2 = retencionV2;
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
