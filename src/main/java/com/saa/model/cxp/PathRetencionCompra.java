package com.saa.model.cxp;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entity PathRetencionCompra.
 * Almacena los paths/adjuntos de la retención de compra (tabla pgs.prcm).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PRCM", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "PathRetencionCompraAll", query = "select e from PathRetencionCompra e"),
	@NamedQuery(name = "PathRetencionCompraId", query = "select e from PathRetencionCompra e where e.id = :id")
})
public class PathRetencionCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne @JoinColumn(name = "RETENCION", referencedColumnName = "ID")
	private RetencionCompra retencion;

	@Basic @Column(name = "PATH", length = 1000)
	private String path;

	@Basic @Column(name = "ALTERNO")
	private Long alterno;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public RetencionCompra getRetencion() { return retencion; }
	public void setRetencion(RetencionCompra retencion) { this.retencion = retencion; }
	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }
	public Long getAlterno() { return alterno; }
	public void setAlterno(Long alterno) { this.alterno = alterno; }
}
