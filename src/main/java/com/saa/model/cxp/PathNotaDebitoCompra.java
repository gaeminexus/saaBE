package com.saa.model.cxp;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entity PathNotaDebitoCompra.
 * Almacena los paths/adjuntos de la nota de débito de compra (tabla pgs.ptdc).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PTDC", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "PathNotaDebitoCompraAll", query = "select e from PathNotaDebitoCompra e"),
	@NamedQuery(name = "PathNotaDebitoCompraId", query = "select e from PathNotaDebitoCompra e where e.id = :id")
})
public class PathNotaDebitoCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne @JoinColumn(name = "NOTADEBITO", referencedColumnName = "ID")
	private NotaDebitoCompra notaDebito;

	@Basic @Column(name = "PATH", length = 1000)
	private String path;

	@Basic @Column(name = "ALTERNO")
	private Long alterno;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public NotaDebitoCompra getNotaDebito() { return notaDebito; }
	public void setNotaDebito(NotaDebitoCompra notaDebito) { this.notaDebito = notaDebito; }
	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }
	public Long getAlterno() { return alterno; }
	public void setAlterno(Long alterno) { this.alterno = alterno; }
}
