package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Titular;

import jakarta.persistence.*;

/**
 * Entity RetencionCompraV2.
 * Almacena los datos de retención de compra V2 (tabla pgs.rcv2).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RCV2", schema = "PGS")
@NamedQueries({
	@NamedQuery(name = "RetencionCompraV2All", query = "select e from RetencionCompraV2 e"),
	@NamedQuery(name = "RetencionCompraV2Id", query = "select e from RetencionCompraV2 e where e.id = :id")
})
public class RetencionCompraV2 implements Serializable {

	@Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Basic @Column(name = "TIPOCOMPROBANTE", length = 10)
	private String tipoComprobante;

	@ManyToOne @JoinColumn(name = "EMPRESA", referencedColumnName = "PJRQCDGO")
	private Empresa empresa;

	@ManyToOne @JoinColumn(name = "PROVEEDOR", referencedColumnName = "TTLRCDGO")
	private Titular proveedor;

	@Basic @Column(name = "TIPODOC", length = 10)
	private String tipoDoc;

	@Basic @Column(name = "PERIODOFISCAL", length = 50)
	private String periodoFiscal;

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

	@Basic @Column(name = "OBSERVACION", length = 2000)
	private String observacion;

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
	public Titular getProveedor() { return proveedor; }
	public void setProveedor(Titular proveedor) { this.proveedor = proveedor; }
	public String getTipoDoc() { return tipoDoc; }
	public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }
	public String getPeriodoFiscal() { return periodoFiscal; }
	public void setPeriodoFiscal(String periodoFiscal) { this.periodoFiscal = periodoFiscal; }
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
	public String getObservacion() { return observacion; }
	public void setObservacion(String observacion) { this.observacion = observacion; }
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
