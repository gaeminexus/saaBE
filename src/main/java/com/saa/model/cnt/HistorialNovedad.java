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
 * C25 — Historial de una novedad o requerimiento (CNT.HSNV). Solo inserción, como CNT.HCPR.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C25-01-seguimiento-novedades.sql. Las FK se guardan como id (no relación):
 * la entidad se serializa directo a JSON y la pantalla arma el detalle con GET /nvdd/{id}/detalle.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HSNV", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "HistorialNovedadAll", query = "select e from HistorialNovedad e"),
	@NamedQuery(name = "HistorialNovedadId", query = "select e from HistorialNovedad e where e.codigo = :id")
})
public class HistorialNovedad implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "HSNVCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Novedad. */
	@Basic
	@Column(name = "NVDDCDGO")
	private Long idNovedad;

	/** Requerimiento, si el cambio es de un requerimiento. */
	@Basic
	@Column(name = "RQNVCDGO")
	private Long idRequerimiento;

	/** CARGA, EDICION, ENVIO, RESPUESTA, ACEPTACION, OBSERVACION, NO_EXISTE, NO_APLICA, APLICACION, AJUSTE, VINCULO, DESVINCULO, DECISION, ADJUNTO, COPIA_EXPEDIENTE, CIERRE, REAPERTURA, ANULACION, REEMPLAZO. */
	@Basic
	@Column(name = "HSNVACCN", length = 20)
	private String accion;

	/** Estado anterior. */
	@Basic
	@Column(name = "HSNVESAN")
	private Long estadoAnterior;

	/** Estado nuevo. */
	@Basic
	@Column(name = "HSNVESNV")
	private Long estadoNuevo;

	/** Motivo o comentario. */
	@Basic
	@Column(name = "HSNVMTVO", length = 2000)
	private String motivo;

	/** Fecha y hora. */
	@Basic
	@Column(name = "HSNVFCHA")
	private LocalDateTime fecha;

	/** Usuario. */
	@Basic
	@Column(name = "HSNVUSAR", length = 50)
	private String usuario;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdNovedad() { return idNovedad; }
	public void setIdNovedad(Long idNovedad) { this.idNovedad = idNovedad; }
	public Long getIdRequerimiento() { return idRequerimiento; }
	public void setIdRequerimiento(Long idRequerimiento) { this.idRequerimiento = idRequerimiento; }
	public String getAccion() { return accion; }
	public void setAccion(String accion) { this.accion = accion; }
	public Long getEstadoAnterior() { return estadoAnterior; }
	public void setEstadoAnterior(Long estadoAnterior) { this.estadoAnterior = estadoAnterior; }
	public Long getEstadoNuevo() { return estadoNuevo; }
	public void setEstadoNuevo(Long estadoNuevo) { this.estadoNuevo = estadoNuevo; }
	public String getMotivo() { return motivo; }
	public void setMotivo(String motivo) { this.motivo = motivo; }
	public LocalDateTime getFecha() { return fecha; }
	public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
	public String getUsuario() { return usuario; }
	public void setUsuario(String usuario) { this.usuario = usuario; }
}
