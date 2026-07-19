package com.saa.model.cxp;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entity PathNotaCreditoCompra.
 * Almacena los paths/adjuntos de la nota de crédito de compra (tabla pgs.ptcv).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PTCV", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "PathNotaCreditoCompraAll", query = "select e from PathNotaCreditoCompra e"),
	@NamedQuery(name = "PathNotaCreditoCompraId", query = "select e from PathNotaCreditoCompra e where e.id = :id")
})
public class PathNotaCreditoCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne @JoinColumn(name = "NOTACREDITO", referencedColumnName = "ID")
	private NotaCreditoCompra notaCredito;

	@Basic @Column(name = "PATH", length = 1000)
	private String path;

	@Basic @Column(name = "ALTERNO")
	private Long alterno;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public NotaCreditoCompra getNotaCredito() { return notaCredito; }
	public void setNotaCredito(NotaCreditoCompra notaCredito) { this.notaCredito = notaCredito; }
	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }
	public Long getAlterno() { return alterno; }
	public void setAlterno(Long alterno) { this.alterno = alterno; }
}
