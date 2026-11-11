package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * C25 — Requerimiento de una novedad (CNT.RQNV): cada documento, decisión, confirmación, elección o aprobación pedida a un área de ASOPREP.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C25-01-seguimiento-novedades.sql. Las FK se guardan como id (no relación):
 * la entidad se serializa directo a JSON y la pantalla arma el detalle con GET /nvdd/{id}/detalle.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RQNV", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "RequerimientoNovedadAll", query = "select e from RequerimientoNovedad e"),
	@NamedQuery(name = "RequerimientoNovedadId", query = "select e from RequerimientoNovedad e where e.codigo = :id")
})
public class RequerimientoNovedad implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "RQNVCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Novedad (CNT.NVDD). */
	@Basic
	@Column(name = "NVDDCDGO")
	private Long idNovedad;

	/** Orden dentro de la novedad. */
	@Basic
	@Column(name = "RQNVSCNC")
	private Long secuencia;

	/** DOCUMENTO, DECISION, CONFIRMACION, ELECCION, APROBACION. */
	@Basic
	@Column(name = "RQNVTPRQ", length = 20)
	private String tipo;

	/** Área de ASOPREP que responde. */
	@Basic
	@Column(name = "RQNVARRS", length = 30)
	private String area;

	/** Qué se pide. */
	@Basic
	@Column(name = "RQNVDSCR", length = 2000)
	private String descripcion;

	/** PRESTAMO, OPERACION, ENTIDAD, FILA_EXTRACTO, ORDEN, CHEQUE, FACTURA, EMPLEADO, CARGA, OTRO. */
	@Basic
	@Column(name = "RQNVTPRF", length = 20)
	private String tipoReferencia;

	/** Número interno de referencia (nunca nombre ni cédula). */
	@Basic
	@Column(name = "RQNVRFRN", length = 200)
	private String referencia;

	/** Valor del caso. */
	@Basic
	@Column(name = "RQNVVLOR")
	private Double valor;

	/** Fecha del hecho. */
	@Basic
	@Column(name = "RQNVFCRF")
	private LocalDate fechaReferencia;

	/** Opciones de una ELECCION, separadas por |. */
	@Basic
	@Column(name = "RQNVOPCN", length = 500)
	private String opciones;

	/** Supuesto que rige mientras no hay respuesta. */
	@Basic
	@Column(name = "RQNVSPST", length = 1000)
	private String supuesto;

	/** 1 PENDIENTE, 2 RESPONDIDO, 3 ACEPTADO, 4 OBSERVADO, 5 NO_EXISTE, 6 APLICADO, 7 NO_APLICA, 8 ANULADO. */
	@Basic
	@Column(name = "RQNVESTD")
	private Long estado;

	/** Fecha de registro. */
	@Basic
	@Column(name = "RQNVFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que registró. */
	@Basic
	@Column(name = "RQNVUSAR", length = 50)
	private String usuarioRegistro;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdNovedad() { return idNovedad; }
	public void setIdNovedad(Long idNovedad) { this.idNovedad = idNovedad; }
	public Long getSecuencia() { return secuencia; }
	public void setSecuencia(Long secuencia) { this.secuencia = secuencia; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public String getArea() { return area; }
	public void setArea(String area) { this.area = area; }
	public String getDescripcion() { return descripcion; }
	public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
	public String getTipoReferencia() { return tipoReferencia; }
	public void setTipoReferencia(String tipoReferencia) { this.tipoReferencia = tipoReferencia; }
	public String getReferencia() { return referencia; }
	public void setReferencia(String referencia) { this.referencia = referencia; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public LocalDate getFechaReferencia() { return fechaReferencia; }
	public void setFechaReferencia(LocalDate fechaReferencia) { this.fechaReferencia = fechaReferencia; }
	public String getOpciones() { return opciones; }
	public void setOpciones(String opciones) { this.opciones = opciones; }
	public String getSupuesto() { return supuesto; }
	public void setSupuesto(String supuesto) { this.supuesto = supuesto; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
