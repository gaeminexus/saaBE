package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Titular;

import jakarta.persistence.*;

/**
 * Entity NotaDebitoCompra.
 * Almacena los datos de nota de débito de compra (tabla pgs.ntdc).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NTDC", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "NotaDebitoCompraAll", query = "select e from NotaDebitoCompra e"),
	@NamedQuery(name = "NotaDebitoCompraId", query = "select e from NotaDebitoCompra e where e.id = :id")
})
public class NotaDebitoCompra implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Basic @Column(name = "TIPOCOMPROBANTE", length = 10)
	private String tipoComprobante;

	@ManyToOne @JoinColumn(name = "EMPRESA", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;

	@ManyToOne @JoinColumn(name = "TITULAR", referencedColumnName = "TTLRCDGO")
	private Titular titular;

	@Basic @Column(name = "TIPODOC", length = 10)
	private String tipoDoc;

	@Basic @Column(name = "NUMERO", length = 100)
	private String numero;

	@Basic @Column(name = "NUMESTABLECIMIENTO", length = 500)
	private String numEstablecimiento;

	@Basic @Column(name = "NUMPTOEMISION", length = 500)
	private String numPtoEmision;

	@Basic @Column(name = "SECUENCIAL", length = 1000)
	private String secuencial;

	@Basic @Column(name = "AMBIENTE")
	private Long ambiente;

	@Basic @Column(name = "CLAVE", length = 100)
	private String clave;

	@Basic @Column(name = "FECHA")
	private LocalDateTime fecha;

	@Basic @Column(name = "TIPODOCMODIFICADO", length = 45)
	private String tipoDocModificado;

	@Basic @Column(name = "NUMDOCMODIFICADO", length = 500)
	private String numDocModificado;

	@Basic @Column(name = "FECHAEMISIONDM")
	private LocalDateTime fechaEmisionDM;

	@Basic @Column(name = "OBSERVACION", length = 2000)
	private String observacion;

	@Basic @Column(name = "SUBTOTAL")
	private Double subtotal;

	@Basic @Column(name = "SUBCERO")
	private Double subcero;

	@Basic @Column(name = "PIVA")
	private Double pIVA;

	@Basic @Column(name = "VIVA")
	private Double vIVA;

	@Basic @Column(name = "VICE")
	private Double vICE;

	@Basic @Column(name = "VIRBPNR")
	private Double vIRBPNR;

	@Basic @Column(name = "DESCUENTO")
	private Double descuento;

	@Basic @Column(name = "PORDESCUENTO")
	private Double porDescuento;

	@Basic @Column(name = "PROPINA")
	private Double propina;

	@Basic @Column(name = "SUBSIDIO")
	private Double subsidio;

	@Basic @Column(name = "TOTAL")
	private Double total;

	@Basic @Column(name = "PTOEMISION")
	private Long ptoEmision;

	@ManyToOne @JoinColumn(name = "USUARIO", referencedColumnName = "PJRQCDGO")
	private Usuario usuario;

	@Basic @Column(name = "PATHGEN", length = 2000)
	private String pathGen;

	@Basic @Column(name = "AUTORIZACION", length = 1000)
	private String autorizacion;

	@Basic @Column(name = "FECHAAUTORIZACION")
	private LocalDateTime fechaAutorizacion;

	@Basic @Column(name = "ESTADO")
	private Long estado;

	@Basic @Column(name = "ESTADOEMISION")
	private Long estadoEmision;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getTipoComprobante() { return tipoComprobante; }
	public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }
	public Empresa getEmpresa() { return empresa; }
	public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
	public Titular getTitular() { return titular; }
	public void setTitular(Titular titular) { this.titular = titular; }
	public String getTipoDoc() { return tipoDoc; }
	public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }
	public String getNumero() { return numero; }
	public void setNumero(String numero) { this.numero = numero; }
	public String getNumEstablecimiento() { return numEstablecimiento; }
	public void setNumEstablecimiento(String numEstablecimiento) { this.numEstablecimiento = numEstablecimiento; }
	public String getNumPtoEmision() { return numPtoEmision; }
	public void setNumPtoEmision(String numPtoEmision) { this.numPtoEmision = numPtoEmision; }
	public String getSecuencial() { return secuencial; }
	public void setSecuencial(String secuencial) { this.secuencial = secuencial; }
	public Long getAmbiente() { return ambiente; }
	public void setAmbiente(Long ambiente) { this.ambiente = ambiente; }
	public String getClave() { return clave; }
	public void setClave(String clave) { this.clave = clave; }
	public LocalDateTime getFecha() { return fecha; }
	public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
	public String getTipoDocModificado() { return tipoDocModificado; }
	public void setTipoDocModificado(String tipoDocModificado) { this.tipoDocModificado = tipoDocModificado; }
	public String getNumDocModificado() { return numDocModificado; }
	public void setNumDocModificado(String numDocModificado) { this.numDocModificado = numDocModificado; }
	public LocalDateTime getFechaEmisionDM() { return fechaEmisionDM; }
	public void setFechaEmisionDM(LocalDateTime fechaEmisionDM) { this.fechaEmisionDM = fechaEmisionDM; }
	public String getObservacion() { return observacion; }
	public void setObservacion(String observacion) { this.observacion = observacion; }
	public Double getSubtotal() { return subtotal; }
	public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
	public Double getSubcero() { return subcero; }
	public void setSubcero(Double subcero) { this.subcero = subcero; }
	public Double getpIVA() { return pIVA; }
	public void setpIVA(Double pIVA) { this.pIVA = pIVA; }
	public Double getvIVA() { return vIVA; }
	public void setvIVA(Double vIVA) { this.vIVA = vIVA; }
	public Double getvICE() { return vICE; }
	public void setvICE(Double vICE) { this.vICE = vICE; }
	public Double getvIRBPNR() { return vIRBPNR; }
	public void setvIRBPNR(Double vIRBPNR) { this.vIRBPNR = vIRBPNR; }
	public Double getDescuento() { return descuento; }
	public void setDescuento(Double descuento) { this.descuento = descuento; }
	public Double getPorDescuento() { return porDescuento; }
	public void setPorDescuento(Double porDescuento) { this.porDescuento = porDescuento; }
	public Double getPropina() { return propina; }
	public void setPropina(Double propina) { this.propina = propina; }
	public Double getSubsidio() { return subsidio; }
	public void setSubsidio(Double subsidio) { this.subsidio = subsidio; }
	public Double getTotal() { return total; }
	public void setTotal(Double total) { this.total = total; }
	public Long getPtoEmision() { return ptoEmision; }
	public void setPtoEmision(Long ptoEmision) { this.ptoEmision = ptoEmision; }
	public Usuario getUsuario() { return usuario; }
	public void setUsuario(Usuario usuario) { this.usuario = usuario; }
	public String getPathGen() { return pathGen; }
	public void setPathGen(String pathGen) { this.pathGen = pathGen; }
	public String getAutorizacion() { return autorizacion; }
	public void setAutorizacion(String autorizacion) { this.autorizacion = autorizacion; }
	public LocalDateTime getFechaAutorizacion() { return fechaAutorizacion; }
	public void setFechaAutorizacion(LocalDateTime fechaAutorizacion) { this.fechaAutorizacion = fechaAutorizacion; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public Long getEstadoEmision() { return estadoEmision; }
	public void setEstadoEmision(Long estadoEmision) { this.estadoEmision = estadoEmision; }
}
