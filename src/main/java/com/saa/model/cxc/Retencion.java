/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
package com.saa.model.cxc;

import java.io.Serializable;
import java.time.LocalDateTime;

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
 * Entity Retencion.
 * Almacena los datos de retenciones.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RTNC", schema = "CBR")
@NamedQueries({
	@NamedQuery(name = "RetencionAll", query = "select e from Retencion e"),
	@NamedQuery(name = "RetencionId", query = "select e from Retencion e where e.id = :id")
})
public class Retencion implements Serializable {

	@Basic
	@Id
	@Column(name = "ID", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Basic
	@Column(name = "TIPOCOMPROBANTE", length = 10)
	private String tipoComprobante;
	
	@ManyToOne
	@JoinColumn(name = "FACTURADOR", referencedColumnName = "ID")
	private Facturador facturador;
	
	@ManyToOne
	@JoinColumn(name = "PROVEEDOR", referencedColumnName = "TTLRCDGO")
	private Titular proveedor;
	
	@Basic
	@Column(name = "TIPODOC", length = 10)
	private String tipoDoc;
	
	@Basic
	@Column(name = "PERIODOFISCAL", length = 50)
	private String periodoFiscal;
	
	@Basic
	@Column(name = "NUMERO", length = 100)
	private String numero;
	
	@Basic
	@Column(name = "NUMESTABLECIMIENTO", length = 500)
	private String numEstablecimiento;
	
	@Basic
	@Column(name = "NUMPTOEMISION", length = 500)
	private String numPtoEmision;
	
	@Basic
	@Column(name = "SECUENCIAL", length = 1000)
	private String secuencial;
	
	@Basic
	@Column(name = "AMBIENTE")
	private Long ambiente;
	
	@Basic
	@Column(name = "CLAVE", length = 100)
	private String clave;
	
	@Basic
	@Column(name = "FECHA")
	private LocalDateTime fecha;
	
	@Basic
	@Column(name = "OBSERVACION", length = 2000)
	private String observacion;
	
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
	
	@Basic
	@Column(name = "PATHGEN", length = 2000)
	private String pathGen;
	
	@Basic
	@Column(name = "AUTORIZACION", length = 1000)
	private String autorizacion;
	
	@Basic
	@Column(name = "FECHAAUTORIZACION")
	private LocalDateTime fechaAutorizacion;
	
	@Basic
	@Column(name = "ESTADO")
	private Long estado;
	
	@Basic
	@Column(name = "ESTADOEMISION")
	private Long estadoEmision;

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

	public Titular getProveedor() {
		return proveedor;
	}

	public void setProveedor(Titular proveedor) {
		this.proveedor = proveedor;
	}

	public String getTipoDoc() {
		return tipoDoc;
	}

	public void setTipoDoc(String tipoDoc) {
		this.tipoDoc = tipoDoc;
	}

	public String getPeriodoFiscal() {
		return periodoFiscal;
	}

	public void setPeriodoFiscal(String periodoFiscal) {
		this.periodoFiscal = periodoFiscal;
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

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public String getObservacion() {
		return observacion;
	}

	public void setObservacion(String observacion) {
		this.observacion = observacion;
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
}
