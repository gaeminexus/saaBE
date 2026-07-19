package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import jakarta.persistence.*;

/**
 * Entity DetalleRetencionCompra.
 * Almacena el detalle de la retención de compra (tabla pgs.drcm).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DRCM", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "DetalleRetencionCompraAll", query = "select e from DetalleRetencionCompra e"),
	@NamedQuery(name = "DetalleRetencionCompraId", query = "select e from DetalleRetencionCompra e where e.id = :id")
})
public class DetalleRetencionCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne @JoinColumn(name = "RETENCION", referencedColumnName = "ID")
	private RetencionCompra retencion;

	@Basic @Column(name = "TIPODOCRETEN", length = 2)
	private String tipoDocReten;

	@Basic @Column(name = "NUMDOCRETEN", length = 100)
	private String numDocReten;

	@Basic @Column(name = "FECHAEMIDOC")
	private LocalDate fechaEmiDoc;

	@Basic @Column(name = "CODIMPUESTO", length = 2)
	private String codImpuesto;

	@Basic @Column(name = "CODRETENCION", length = 100)
	private String codRetencion;

	@Basic @Column(name = "BASEIMPONIBLE")
	private Double baseImponible;

	@Basic @Column(name = "PORCENTAJERETEN")
	private Double porcentajeReten;

	@Basic @Column(name = "VALORRETEN")
	private Double valorReten;

	@Basic @Column(name = "ESTADO")
	private Long estado;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public RetencionCompra getRetencion() { return retencion; }
	public void setRetencion(RetencionCompra retencion) { this.retencion = retencion; }
	public String getTipoDocReten() { return tipoDocReten; }
	public void setTipoDocReten(String tipoDocReten) { this.tipoDocReten = tipoDocReten; }
	public String getNumDocReten() { return numDocReten; }
	public void setNumDocReten(String numDocReten) { this.numDocReten = numDocReten; }
	public LocalDate getFechaEmiDoc() { return fechaEmiDoc; }
	public void setFechaEmiDoc(LocalDate fechaEmiDoc) { this.fechaEmiDoc = fechaEmiDoc; }
	public String getCodImpuesto() { return codImpuesto; }
	public void setCodImpuesto(String codImpuesto) { this.codImpuesto = codImpuesto; }
	public String getCodRetencion() { return codRetencion; }
	public void setCodRetencion(String codRetencion) { this.codRetencion = codRetencion; }
	public Double getBaseImponible() { return baseImponible; }
	public void setBaseImponible(Double baseImponible) { this.baseImponible = baseImponible; }
	public Double getPorcentajeReten() { return porcentajeReten; }
	public void setPorcentajeReten(Double porcentajeReten) { this.porcentajeReten = porcentajeReten; }
	public Double getValorReten() { return valorReten; }
	public void setValorReten(Double valorReten) { this.valorReten = valorReten; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
}
