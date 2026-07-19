package com.saa.model.cxp;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entity DetalleNotaDebitoCompra.
 * Almacena el detalle de la nota de débito de compra (tabla pgs.dtdc).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTDC", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "DetalleNotaDebitoCompraAll", query = "select e from DetalleNotaDebitoCompra e"),
	@NamedQuery(name = "DetalleNotaDebitoCompraId", query = "select e from DetalleNotaDebitoCompra e where e.id = :id")
})
public class DetalleNotaDebitoCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne @JoinColumn(name = "NOTADEBITO", referencedColumnName = "ID")
	private NotaDebitoCompra notaDebito;

	@Basic @Column(name = "DESCRIPCION", length = 500)
	private String descripcion;

	@Basic @Column(name = "CANTIDAD")
	private Double cantidad;

	@Basic @Column(name = "VALOR")
	private Double valor;

	@Basic @Column(name = "SUBTOTAL")
	private Double subTotal;

	@Basic @Column(name = "DESCUENTO")
	private Double descuento;

	@Basic @Column(name = "BASEIMPONIBLE")
	private Double baseImponible;

	@Basic @Column(name = "PORCENTAJEIVA")
	private Long porcentajeIVA;

	@Basic @Column(name = "VALORIVA")
	private Double valorIVA;

	@Basic @Column(name = "PORCENTAJEICE")
	private Long porcentajeICE;

	@Basic @Column(name = "VALORICE")
	private Double valorICE;

	@Basic @Column(name = "SUBSIDIO")
	private Double subsidio;

	@Basic @Column(name = "TOTAL")
	private Double total;

	@Basic @Column(name = "ESTADO")
	private Long estado;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public NotaDebitoCompra getNotaDebito() { return notaDebito; }
	public void setNotaDebito(NotaDebitoCompra notaDebito) { this.notaDebito = notaDebito; }
	public String getDescripcion() { return descripcion; }
	public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
	public Double getCantidad() { return cantidad; }
	public void setCantidad(Double cantidad) { this.cantidad = cantidad; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public Double getSubTotal() { return subTotal; }
	public void setSubTotal(Double subTotal) { this.subTotal = subTotal; }
	public Double getDescuento() { return descuento; }
	public void setDescuento(Double descuento) { this.descuento = descuento; }
	public Double getBaseImponible() { return baseImponible; }
	public void setBaseImponible(Double baseImponible) { this.baseImponible = baseImponible; }
	public Long getPorcentajeIVA() { return porcentajeIVA; }
	public void setPorcentajeIVA(Long porcentajeIVA) { this.porcentajeIVA = porcentajeIVA; }
	public Double getValorIVA() { return valorIVA; }
	public void setValorIVA(Double valorIVA) { this.valorIVA = valorIVA; }
	public Long getPorcentajeICE() { return porcentajeICE; }
	public void setPorcentajeICE(Long porcentajeICE) { this.porcentajeICE = porcentajeICE; }
	public Double getValorICE() { return valorICE; }
	public void setValorICE(Double valorICE) { this.valorICE = valorICE; }
	public Double getSubsidio() { return subsidio; }
	public void setSubsidio(Double subsidio) { this.subsidio = subsidio; }
	public Double getTotal() { return total; }
	public void setTotal(Double total) { this.total = total; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
}
