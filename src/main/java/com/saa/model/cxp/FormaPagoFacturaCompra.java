package com.saa.model.cxp;

import java.io.Serializable;
import jakarta.persistence.*;

/**
 * Entity FormaPagoFacturaCompra.
 * Almacena las formas de pago de la factura de compra (tabla pgs.fpfm).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FPFM", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "FormaPagoFacturaCompraAll", query = "select e from FormaPagoFacturaCompra e"),
	@NamedQuery(name = "FormaPagoFacturaCompraId", query = "select e from FormaPagoFacturaCompra e where e.id = :id")
})
public class FormaPagoFacturaCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne @JoinColumn(name = "FACTURA", referencedColumnName = "ID")
	private FacturaCompra factura;

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
	public FacturaCompra getFactura() { return factura; }
	public void setFactura(FacturaCompra factura) { this.factura = factura; }
	public String getFormaPago() { return formaPago; }
	public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public Long getPlazo() { return plazo; }
	public void setPlazo(Long plazo) { this.plazo = plazo; }
	public String getUnidadTiempo() { return unidadTiempo; }
	public void setUnidadTiempo(String unidadTiempo) { this.unidadTiempo = unidadTiempo; }
}
