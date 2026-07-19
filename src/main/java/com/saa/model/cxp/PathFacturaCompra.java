package com.saa.model.cxp;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entity PathFacturaCompra.
 * Almacena los paths/adjuntos de la factura de compra (tabla pgs.pfcc).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PFCC", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "PathFacturaCompraAll", query = "select e from PathFacturaCompra e"),
	@NamedQuery(name = "PathFacturaCompraId", query = "select e from PathFacturaCompra e where e.id = :id")
})
public class PathFacturaCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne @JoinColumn(name = "FACTURA", referencedColumnName = "ID")
	private FacturaCompra factura;

	@Basic @Column(name = "PATH", length = 1000)
	private String path;

	@Basic @Column(name = "ALTERNO")
	private Long alterno;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public FacturaCompra getFactura() { return factura; }
	public void setFactura(FacturaCompra factura) { this.factura = factura; }
	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }
	public Long getAlterno() { return alterno; }
	public void setAlterno(Long alterno) { this.alterno = alterno; }
}
