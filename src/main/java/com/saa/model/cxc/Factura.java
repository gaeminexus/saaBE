/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
package com.saa.model.cxc;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Titular;

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
 * Entity Factura.
 * Almacena los datos de la factura electrónica.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FCTR", schema = "CBR")
@NamedQueries({
	@NamedQuery(name = "FacturaAll", query = "select e from Factura e"),
	@NamedQuery(name = "FacturaId", query = "select e from Factura e where e.id = :id")
})
public class Factura implements Serializable {

	/**
	 * Id de Tabla.
	 */
	@Basic
	@Id
	@Column(name = "ID", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	/**
	 * Tipo de comprobante (01 = Factura).
	 */
	@Basic
	@Column(name = "TIPOCOMPROBANTE", length = 10)
	private String tipoComprobante;
	
	/**
	 * Facturador (emisor de la factura).
	 */
	@ManyToOne
	@JoinColumn(name = "FACTURADOR", referencedColumnName = "ID")
	private Facturador facturador;
	
	/**
	 * Comprador (cliente que compra).
	 */
	@ManyToOne
	@JoinColumn(name = "COMPRADOR", referencedColumnName = "TTLRCDGO")
	private Titular titular;
	
	/**
	 * Tipo de documento.
	 */
	@Basic
	@Column(name = "TIPODOC", length = 10)
	private String tipoDoc;
	
	/**
	 * Número completo del comprobante.
	 */
	@Basic
	@Column(name = "NUMERO", length = 100)
	private String numero;
	
	/**
	 * Número de establecimiento.
	 */
	@Basic
	@Column(name = "NUMESTABLECIMIENTO", length = 500)
	private String numEstablecimiento;
	
	/**
	 * Número de punto de emisión.
	 */
	@Basic
	@Column(name = "NUMPTOEMISION", length = 500)
	private String numPtoEmision;
	
	/**
	 * Secuencial del comprobante.
	 */
	@Basic
	@Column(name = "SECUENCIAL", length = 1000)
	private String secuencial;
	
	/**
	 * Ambiente (1 = PRUEBA, 2 = PRODUCCIÓN).
	 */
	@Basic
	@Column(name = "AMBIENTE")
	private Long ambiente;
	
	/**
	 * Clave de acceso del comprobante.
	 */
	@Basic
	@Column(name = "CLAVE", length = 100)
	private String clave;
	
	/**
	 * Fecha de emisión de la factura.
	 */
	@Basic
	@Column(name = "FECHA")
	private LocalDate fecha;
	
	/**
	 * Observación o descripción adicional.
	 */
	@Basic
	@Column(name = "OBSERVACION", length = 2000)
	private String observacion;
	
	/**
	 * Subtotal gravado con IVA.
	 */
	@Basic
	@Column(name = "SUBTOTAL")
	private Double subtotal;
	
	/**
	 * Subtotal 0% IVA.
	 */
	@Basic
	@Column(name = "SUBCERO")
	private Double subcero;
	
	/**
	 * Subtotal gravado con IVA 5%.
	 */
	@Basic
	@Column(name = "SUBTOTAL5")
	private Double subtotal5;
	
	/**
	 * Subtotal gravado con IVA 8%.
	 */
	@Basic
	@Column(name = "SUBTOTAL8")
	private Double subtotal8;
	
	/**
	 * Porcentaje de IVA aplicado.
	 */
	@Basic
	@Column(name = "PIVA")
	private Double pIVA;
	
	/**
	 * Valor del IVA.
	 */
	@Basic
	@Column(name = "VIVA")
	private Double vIVA;
	
	/**
	 * Valor del IVA 5%.
	 */
	@Basic
	@Column(name = "VIVA5")
	private Double vIVA5;
	
	/**
	 * Valor del IVA 8%.
	 */
	@Basic
	@Column(name = "VIVA8")
	private Double vIVA8;
	
	/**
	 * Valor del ICE (Impuesto a los Consumos Especiales).
	 */
	@Basic
	@Column(name = "VICE")
	private Double vICE;
	
	/**
	 * Valor IRBPNR (Impuesto Redimible Botellas Plásticas No Retornables).
	 */
	@Basic
	@Column(name = "VIRBPNR")
	private Double vIRBPNR;
	
	/**
	 * Descuento aplicado.
	 */
	@Basic
	@Column(name = "DESCUENTO")
	private Double descuento;
	
	/**
	 * Porcentaje de descuento.
	 */
	@Basic
	@Column(name = "PORDESCUENTO")
	private Double porDescuento;
	
	/**
	 * Propina.
	 */
	@Basic
	@Column(name = "PROPINA")
	private Double propina;
	
	/**
	 * Subsidio aplicado.
	 */
	@Basic
	@Column(name = "SUBSIDIO")
	private Double subsidio;
	
	/**
	 * Total sin subsidio.
	 */
	@Basic
	@Column(name = "TOTALSINSUB")
	private Double totalSinSub;
	
	/**
	 * Ahorro por subsidio.
	 */
	@Basic
	@Column(name = "AHORROSUB")
	private Double ahorroSub;
	
	/**
	 * Total de la factura.
	 */
	@Basic
	@Column(name = "TOTAL")
	private Double total;
	
	/**
	 * Punto de emisión (referencia).
	 */
	@ManyToOne
	@JoinColumn(name = "PTOEMISION", referencedColumnName = "ID")
	private PuntoEmision ptoEmision;
	
	/**
	 * Usuario que generó la factura.
	 */
	@ManyToOne
	@JoinColumn(name = "USUARIO", referencedColumnName = "PJRQCDGO")
	private Usuario usuario;
	
	/**
	 * Path donde se generó el XML.
	 */
	@Basic
	@Column(name = "PATHGEN", length = 2000)
	private String pathGen;
	
	/**
	 * Número de autorización del SRI.
	 */
	@Basic
	@Column(name = "AUTORIZACION", length = 1000)
	private String autorizacion;
	
	/**
	 * Fecha de autorización del SRI.
	 */
	@Basic
	@Column(name = "FECHAAUTORIZACION")
	private LocalDateTime fechaAutorizacion;
	
	/**
	 * Forma de pago.
	 */
	@Basic
	@Column(name = "FORMAPAGO")
	private Long formaPago;
	
	/**
	 * Estado del registro.
	 */
	@Basic
	@Column(name = "ESTADO")
	private Long estado;
	
	/**
	 * Estado de emisión (pendiente, autorizado, rechazado).
	 */
	@Basic
	@Column(name = "ESTADOEMISION")
	private Long estadoEmision;
	
	/**
	 * Asiento contable generado al autorizar la factura.
	 */
	@ManyToOne
	@JoinColumn(name = "ASIENTO", referencedColumnName = "ASNTCDGO")
	private Asiento asiento;
	
	/**
	 * Empresa contable a la que pertenece el facturador.
	 */
	@ManyToOne
	@JoinColumn(name = "EMPRESA", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;

	/**
	 * Motivo de anulación de la factura.
	 */
	@Basic
	@Column(name = "MOTIVOANULACION", length = 1000)
	private String motivoAnulacion;

	/**
	 * Fecha en que se anuló la factura.
	 */
	@Basic
	@Column(name = "FECHAANULACION")
	private java.time.LocalDateTime fechaAnulacion;

	/**
	 * Usuario que realizó la anulación.
	 */
	@Basic
	@Column(name = "USUARIOANULACION", length = 200)
	private String usuarioAnulacion;

	// Getters y Setters
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTipoComprobante() {
		return tipoComprobante;
	}

	public void setTipoComprobante(String tipoComprobante) {
		this.tipoComprobante = tipoComprobante;
	}

	public Facturador getFacturador() {
		return facturador;
	}

	public void setFacturador(Facturador facturador) {
		this.facturador = facturador;
	}

	public Titular getTitular() {
		return titular;
	}

	public void setTitular(Titular titular) {
		this.titular = titular;
	}

	public String getTipoDoc() {
		return tipoDoc;
	}

	public void setTipoDoc(String tipoDoc) {
		this.tipoDoc = tipoDoc;
	}

	public String getNumero() {
		return numero;
	}

	public void setNumero(String numero) {
		this.numero = numero;
	}

	public String getNumEstablecimiento() {
		return numEstablecimiento;
	}

	public void setNumEstablecimiento(String numEstablecimiento) {
		this.numEstablecimiento = numEstablecimiento;
	}

	public String getNumPtoEmision() {
		return numPtoEmision;
	}

	public void setNumPtoEmision(String numPtoEmision) {
		this.numPtoEmision = numPtoEmision;
	}

	public String getSecuencial() {
		return secuencial;
	}

	public void setSecuencial(String secuencial) {
		this.secuencial = secuencial;
	}

	public Long getAmbiente() {
		return ambiente;
	}

	public void setAmbiente(Long ambiente) {
		this.ambiente = ambiente;
	}

	public String getClave() {
		return clave;
	}

	public void setClave(String clave) {
		this.clave = clave;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public void setFecha(LocalDate fecha) {
		this.fecha = fecha;
	}

	public String getObservacion() {
		return observacion;
	}

	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}

	public Double getSubtotal() {
		return subtotal;
	}

	public void setSubtotal(Double subtotal) {
		this.subtotal = subtotal;
	}

	public Double getSubcero() {
		return subcero;
	}

	public void setSubcero(Double subcero) {
		this.subcero = subcero;
	}

	public Double getSubtotal5() {
		return subtotal5;
	}

	public void setSubtotal5(Double subtotal5) {
		this.subtotal5 = subtotal5;
	}

	public Double getSubtotal8() {
		return subtotal8;
	}

	public void setSubtotal8(Double subtotal8) {
		this.subtotal8 = subtotal8;
	}

	public Double getpIVA() {
		return pIVA;
	}

	public void setpIVA(Double pIVA) {
		this.pIVA = pIVA;
	}

	public Double getvIVA() {
		return vIVA;
	}

	public void setvIVA(Double vIVA) {
		this.vIVA = vIVA;
	}

	public Double getvIVA5() {
		return vIVA5;
	}

	public void setvIVA5(Double vIVA5) {
		this.vIVA5 = vIVA5;
	}

	public Double getvIVA8() {
		return vIVA8;
	}

	public void setvIVA8(Double vIVA8) {
		this.vIVA8 = vIVA8;
	}

	public Double getvICE() {
		return vICE;
	}

	public void setvICE(Double vICE) {
		this.vICE = vICE;
	}

	public Double getvIRBPNR() {
		return vIRBPNR;
	}

	public void setvIRBPNR(Double vIRBPNR) {
		this.vIRBPNR = vIRBPNR;
	}

	public Double getDescuento() {
		return descuento;
	}

	public void setDescuento(Double descuento) {
		this.descuento = descuento;
	}

	public Double getPorDescuento() {
		return porDescuento;
	}

	public void setPorDescuento(Double porDescuento) {
		this.porDescuento = porDescuento;
	}

	public Double getPropina() {
		return propina;
	}

	public void setPropina(Double propina) {
		this.propina = propina;
	}

	public Double getSubsidio() {
		return subsidio;
	}

	public void setSubsidio(Double subsidio) {
		this.subsidio = subsidio;
	}

	public Double getTotalSinSub() {
		return totalSinSub;
	}

	public void setTotalSinSub(Double totalSinSub) {
		this.totalSinSub = totalSinSub;
	}

	public Double getAhorroSub() {
		return ahorroSub;
	}

	public void setAhorroSub(Double ahorroSub) {
		this.ahorroSub = ahorroSub;
	}

	public Double getTotal() {
		return total;
	}

	public void setTotal(Double total) {
		this.total = total;
	}

	public PuntoEmision getPtoEmision() {
		return ptoEmision;
	}

	public void setPtoEmision(PuntoEmision ptoEmision) {
		this.ptoEmision = ptoEmision;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public String getPathGen() {
		return pathGen;
	}

	public void setPathGen(String pathGen) {
		this.pathGen = pathGen;
	}

	public String getAutorizacion() {
		return autorizacion;
	}

	public void setAutorizacion(String autorizacion) {
		this.autorizacion = autorizacion;
	}

	public LocalDateTime getFechaAutorizacion() {
		return fechaAutorizacion;
	}

	public void setFechaAutorizacion(LocalDateTime fechaAutorizacion) {
		this.fechaAutorizacion = fechaAutorizacion;
	}

	public Long getFormaPago() {
		return formaPago;
	}

	public void setFormaPago(Long formaPago) {
		this.formaPago = formaPago;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}

	public Long getEstadoEmision() {
		return estadoEmision;
	}

	public void setEstadoEmision(Long estadoEmision) {
		this.estadoEmision = estadoEmision;
	}

	public Asiento getAsiento() {
		return asiento;
	}

	public void setAsiento(Asiento asiento) {
		this.asiento = asiento;
	}
	
	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public String getMotivoAnulacion() {
		return motivoAnulacion;
	}

	public void setMotivoAnulacion(String motivoAnulacion) {
		this.motivoAnulacion = motivoAnulacion;
	}

	public java.time.LocalDateTime getFechaAnulacion() {
		return fechaAnulacion;
	}

	public void setFechaAnulacion(java.time.LocalDateTime fechaAnulacion) {
		this.fechaAnulacion = fechaAnulacion;
	}

	public String getUsuarioAnulacion() {
		return usuarioAnulacion;
	}

	public void setUsuarioAnulacion(String usuarioAnulacion) {
		this.usuarioAnulacion = usuarioAnulacion;
	}
}
