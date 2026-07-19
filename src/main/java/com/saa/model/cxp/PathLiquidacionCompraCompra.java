package com.saa.model.cxp;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entity PathLiquidacionCompraCompra.
 * Almacena los paths/adjuntos de la liquidación en compra (tabla pgs.plcc).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PLCC", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "PathLiquidacionCompraCompraAll", query = "select e from PathLiquidacionCompraCompra e"),
	@NamedQuery(name = "PathLiquidacionCompraCompraId", query = "select e from PathLiquidacionCompraCompra e where e.id = :id")
})
public class PathLiquidacionCompraCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne @JoinColumn(name = "LIQUIDACION", referencedColumnName = "ID")
	private LiquidacionCompraCompra liquidacion;

	@Basic @Column(name = "PATH", length = 1000)
	private String path;

	@Basic @Column(name = "ALTERNO")
	private Long alterno;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public LiquidacionCompraCompra getLiquidacion() { return liquidacion; }
	public void setLiquidacion(LiquidacionCompraCompra liquidacion) { this.liquidacion = liquidacion; }
	public String getPath() { return path; }
	public void setPath(String path) { this.path = path; }
	public Long getAlterno() { return alterno; }
	public void setAlterno(Long alterno) { this.alterno = alterno; }
}
