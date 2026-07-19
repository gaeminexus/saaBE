package com.saa.model.cxp;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entity FormaPagoLiquidacionCompraCompra.
 * Almacena las formas de pago de la liquidación en compra (tabla pgs.fplm).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FPLM", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "FormaPagoLiquidacionCompraCompraAll", query = "select e from FormaPagoLiquidacionCompraCompra e"),
	@NamedQuery(name = "FormaPagoLiquidacionCompraCompraId", query = "select e from FormaPagoLiquidacionCompraCompra e where e.id = :id")
})
public class FormaPagoLiquidacionCompraCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne @JoinColumn(name = "LIQUIDACION", referencedColumnName = "ID")
	private LiquidacionCompraCompra liquidacion;

	@Basic @Column(name = "FORMAPAGO", length = 15)
	private String formaPago;

	@Basic @Column(name = "VALOR")
	private Double valor;

	@Basic @Column(name = "PLAZO")
	private Long plazo;

	@Basic @Column(name = "UNIDADTIEMPO", length = 100)
	private String unidadTiempo;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public LiquidacionCompraCompra getLiquidacion() { return liquidacion; }
	public void setLiquidacion(LiquidacionCompraCompra liquidacion) { this.liquidacion = liquidacion; }
	public String getFormaPago() { return formaPago; }
	public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public Long getPlazo() { return plazo; }
	public void setPlazo(Long plazo) { this.plazo = plazo; }
	public String getUnidadTiempo() { return unidadTiempo; }
	public void setUnidadTiempo(String unidadTiempo) { this.unidadTiempo = unidadTiempo; }
}
