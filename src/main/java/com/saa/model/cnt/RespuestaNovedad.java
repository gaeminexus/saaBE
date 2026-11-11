package com.saa.model.cnt;

import java.io.Serializable;
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
 * C25 — Respuesta de ASOPREP a un requerimiento (CNT.RSNV), con su revisión (aceptar u observar) por otro usuario.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C25-01-seguimiento-novedades.sql. Las FK se guardan como id (no relación):
 * la entidad se serializa directo a JSON y la pantalla arma el detalle con GET /nvdd/{id}/detalle.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RSNV", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "RespuestaNovedadAll", query = "select e from RespuestaNovedad e"),
	@NamedQuery(name = "RespuestaNovedadId", query = "select e from RespuestaNovedad e where e.codigo = :id")
})
public class RespuestaNovedad implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "RSNVCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Requerimiento (CNT.RQNV). */
	@Basic
	@Column(name = "RQNVCDGO")
	private Long idRequerimiento;

	/** Decisión (CNT.DCSN) que la respuesta constituye u origina. */
	@Basic
	@Column(name = "DCSNCDGO")
	private Long idDecision;

	/** Fecha de la respuesta. */
	@Basic
	@Column(name = "RSNVFCHA")
	private LocalDateTime fecha;

	/** SAA, CORREO, ACTA, REUNION. */
	@Basic
	@Column(name = "RSNVCNAL", length = 20)
	private String canal;

	/** Área que responde. */
	@Basic
	@Column(name = "RSNVARRS", length = 30)
	private String area;

	/** Texto de la respuesta. */
	@Basic
	@Column(name = "RSNVTXTO", length = 4000)
	private String texto;

	/** Opción elegida o SI/NO. */
	@Basic
	@Column(name = "RSNVOPCN", length = 100)
	private String opcion;

	/** 1 RECIBIDA, 2 ACEPTADA, 3 OBSERVADA, 4 ANULADA. */
	@Basic
	@Column(name = "RSNVESTD")
	private Long estado;

	/** Fecha de revisión. */
	@Basic
	@Column(name = "RSNVFCRV")
	private LocalDateTime fechaRevision;

	/** Usuario que revisó. */
	@Basic
	@Column(name = "RSNVUSRV", length = 50)
	private String usuarioRevision;

	/** Observación de la revisión. */
	@Basic
	@Column(name = "RSNVOBRV", length = 2000)
	private String observacionRevision;

	/** Fecha de registro. */
	@Basic
	@Column(name = "RSNVFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que registró la respuesta. */
	@Basic
	@Column(name = "RSNVUSAR", length = 50)
	private String usuarioRegistro;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdRequerimiento() { return idRequerimiento; }
	public void setIdRequerimiento(Long idRequerimiento) { this.idRequerimiento = idRequerimiento; }
	public Long getIdDecision() { return idDecision; }
	public void setIdDecision(Long idDecision) { this.idDecision = idDecision; }
	public LocalDateTime getFecha() { return fecha; }
	public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
	public String getCanal() { return canal; }
	public void setCanal(String canal) { this.canal = canal; }
	public String getArea() { return area; }
	public void setArea(String area) { this.area = area; }
	public String getTexto() { return texto; }
	public void setTexto(String texto) { this.texto = texto; }
	public String getOpcion() { return opcion; }
	public void setOpcion(String opcion) { this.opcion = opcion; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public LocalDateTime getFechaRevision() { return fechaRevision; }
	public void setFechaRevision(LocalDateTime fechaRevision) { this.fechaRevision = fechaRevision; }
	public String getUsuarioRevision() { return usuarioRevision; }
	public void setUsuarioRevision(String usuarioRevision) { this.usuarioRevision = usuarioRevision; }
	public String getObservacionRevision() { return observacionRevision; }
	public void setObservacionRevision(String observacionRevision) { this.observacionRevision = observacionRevision; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
