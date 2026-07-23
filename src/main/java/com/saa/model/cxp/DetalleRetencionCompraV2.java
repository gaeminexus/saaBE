/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;

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
 * Entity DetalleRetencionCompraV2.
 * Almacena el detalle de retenciones de compra versión 2 recibidas (CXP).
 * Tabla: PGS.DRC2
 * Cada registro representa un impuesto retenido sobre un documento sustento
 * recibido de un proveedor.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DRC2", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "DetalleRetencionCompraV2All", query = "select e from DetalleRetencionCompraV2 e"),
	@NamedQuery(name = "DetalleRetencionCompraV2Id",  query = "select e from DetalleRetencionCompraV2 e where e.id = :id")
})
public class DetalleRetencionCompraV2 implements Serializable {

	/**
	 * ID de la entidad (Llave Primaria, generada por secuencia de Oracle).
	 */
	@Basic
	@Id
	@Column(name = "ID", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Retención Compra V2 a la que pertenece este detalle (FK → PGS.RCV2).
	 */
	@ManyToOne
	@JoinColumn(name = "RETENCIONV2", referencedColumnName = "ID")
	private RetencionCompraV2 retencionCompraV2;

	/**
	 * Tipo de documento sustento de la retención (ej: "01"=Factura).
	 */
	@Basic
	@Column(name = "TIPODOCRETEN", length = 2)
	private String tipoDocReten;

	/**
	 * Número del documento sustento (ej: "001-001-000000123").
	 */
	@Basic
	@Column(name = "NUMDOCRETEN", length = 100)
	private String numDocReten;

	/**
	 * Fecha de emisión del documento sustento.
	 */
	@Basic
	@Column(name = "FECHAEMIDOC")
	private LocalDate fechaEmiDoc;

	/**
	 * Fecha de registro del detalle.
	 */
	@Basic
	@Column(name = "FECHAREG")
	private LocalDate fechaReg;

	/**
	 * Número de autorización del documento sustento.
	 */
	@Basic
	@Column(name = "DOCRESAUTORIZACION", length = 100)
	private String docResAutorizacion;

	/**
	 * Total sin impuestos del documento sustento.
	 */
	@Basic
	@Column(name = "DOCRESTSINIMPUESTOS")
	private Double docResTotalSinImpuestos;

	/**
	 * Total IVA 0% del documento sustento.
	 */
	@Basic
	@Column(name = "DOCRESIVACERO")
	private Double docResIvaCero;

	/**
	 * Base imponible con IVA del documento sustento.
	 */
	@Basic
	@Column(name = "DOCRESPORIVA")
	private Double docResPorIva;

	/**
	 * Valor total de IVA del documento sustento.
	 */
	@Basic
	@Column(name = "DOCRESTOTALIVA")
	private Double docResTotalIva;

	/**
	 * Importe total del documento sustento.
	 */
	@Basic
	@Column(name = "DOCRESTOTAL")
	private Double docResTotal;

	/**
	 * Forma de pago del documento sustento (código SRI, ej: "01"=Sin utilización sistema financiero).
	 */
	@Basic
	@Column(name = "DOCRESFORPAGO", length = 2)
	private String docResForPago;

	/**
	 * Código del impuesto retenido (ej: "1"=Renta, "2"=IVA).
	 */
	@Basic
	@Column(name = "CODIMPUESTO", length = 2)
	private String codImpuesto;

	/**
	 * Código de la retención según catálogo SRI (ej: "303", "312", "725").
	 */
	@Basic
	@Column(name = "CODRETENCION", length = 100)
	private String codRetencion;

	/**
	 * Base imponible sobre la que se aplica la retención.
	 */
	@Basic
	@Column(name = "BASEIMPONIBLE")
	private Double baseImponible;

	/**
	 * Porcentaje de retención aplicado.
	 */
	@Basic
	@Column(name = "PORCENTAJERETEN")
	private Double porcentajeReten;

	/**
	 * Valor retenido (baseImponible * porcentajeReten / 100).
	 */
	@Basic
	@Column(name = "VALORRETEN")
	private Double valorReten;

	/**
	 * Estado del registro (1=Activo, 0=Inactivo).
	 */
	@Basic
	@Column(name = "ESTADO")
	private Long estado;

	// ─── Getters y Setters ────────────────────────────────────────────────────

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public RetencionCompraV2 getRetencionCompraV2() { return retencionCompraV2; }
	public void setRetencionCompraV2(RetencionCompraV2 retencionCompraV2) { this.retencionCompraV2 = retencionCompraV2; }

	public String getTipoDocReten() { return tipoDocReten; }
	public void setTipoDocReten(String tipoDocReten) { this.tipoDocReten = tipoDocReten; }

	public String getNumDocReten() { return numDocReten; }
	public void setNumDocReten(String numDocReten) { this.numDocReten = numDocReten; }

	public LocalDate getFechaEmiDoc() { return fechaEmiDoc; }
	public void setFechaEmiDoc(LocalDate fechaEmiDoc) { this.fechaEmiDoc = fechaEmiDoc; }

	public LocalDate getFechaReg() { return fechaReg; }
	public void setFechaReg(LocalDate fechaReg) { this.fechaReg = fechaReg; }

	public String getDocResAutorizacion() { return docResAutorizacion; }
	public void setDocResAutorizacion(String docResAutorizacion) { this.docResAutorizacion = docResAutorizacion; }

	public Double getDocResTotalSinImpuestos() { return docResTotalSinImpuestos; }
	public void setDocResTotalSinImpuestos(Double docResTotalSinImpuestos) { this.docResTotalSinImpuestos = docResTotalSinImpuestos; }

	public Double getDocResIvaCero() { return docResIvaCero; }
	public void setDocResIvaCero(Double docResIvaCero) { this.docResIvaCero = docResIvaCero; }

	public Double getDocResPorIva() { return docResPorIva; }
	public void setDocResPorIva(Double docResPorIva) { this.docResPorIva = docResPorIva; }

	public Double getDocResTotalIva() { return docResTotalIva; }
	public void setDocResTotalIva(Double docResTotalIva) { this.docResTotalIva = docResTotalIva; }

	public Double getDocResTotal() { return docResTotal; }
	public void setDocResTotal(Double docResTotal) { this.docResTotal = docResTotal; }

	public String getDocResForPago() { return docResForPago; }
	public void setDocResForPago(String docResForPago) { this.docResForPago = docResForPago; }

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
