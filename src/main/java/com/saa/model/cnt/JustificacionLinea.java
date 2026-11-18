package com.saa.model.cnt;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * C34 — Justificación (total o parcial) de una línea por justificar (CNT.JSNV). Sin cuatro ojos: vigente al registrarla; se anula con motivo.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C34-01-novedades-v2.sql. Las FK se guardan como id (no relación), como en C25.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "JSNV", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "JustificacionLineaAll", query = "select e from JustificacionLinea e"),
	@NamedQuery(name = "JustificacionLineaId", query = "select e from JustificacionLinea e where e.codigo = :id")
})
public class JustificacionLinea implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "JSNVCDGO")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Línea justificada. */
	@Basic
	@Column(name = "LNNVCDGO")
	private Long idLinea;

	/** Novedad. */
	@Basic
	@Column(name = "NVDDCDGO")
	private Long idNovedad;

	/** Monto justificado. */
	@Basic
	@Column(name = "JSNVMNTO")
	private Double monto;

	/** RESPALDO, DECISION o CRUCE. */
	@Basic
	@Column(name = "JSNVTPJS", length = 20)
	private String tipo;

	/** Línea cruzada (tipo CRUCE). */
	@Basic
	@Column(name = "LNNVCRUC")
	private Long idLineaCruce;

	/** Fecha del hecho. */
	@Basic
	@Column(name = "JSNVFCHH")
	private LocalDate fechaHecho;

	/** Tipo de documento de respaldo. */
	@Basic
	@Column(name = "JSNVTPDC", length = 30)
	private String tipoDocumento;

	/** Número del documento. */
	@Basic
	@Column(name = "JSNVNMDC", length = 100)
	private String numeroDocumento;

	/** Decisión (tipo DECISION). */
	@Basic
	@Column(name = "DCSNCDGO")
	private Long idDecision;

	/** Explicación. */
	@Basic
	@Column(name = "JSNVTXTO", length = 4000)
	private String texto;

	/** 1 VIGENTE, 2 ANULADA. */
	@Basic
	@Column(name = "JSNVESTD")
	private Long estado;

	/** Fecha de registro. */
	@Basic
	@Column(name = "JSNVFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que registró. */
	@Basic
	@Column(name = "JSNVUSAR", length = 50)
	private String usuarioRegistro;

	/** Fecha de anulación. */
	@Basic
	@Column(name = "JSNVFCAN")
	private LocalDateTime fechaAnulacion;

	/** Usuario que anuló. */
	@Basic
	@Column(name = "JSNVUSAN", length = 50)
	private String usuarioAnulacion;

	/** Motivo de anulación. */
	@Basic
	@Column(name = "JSNVMTAN", length = 1000)
	private String motivoAnulacion;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdLinea() { return idLinea; }
	public void setIdLinea(Long idLinea) { this.idLinea = idLinea; }
	public Long getIdNovedad() { return idNovedad; }
	public void setIdNovedad(Long idNovedad) { this.idNovedad = idNovedad; }
	public Double getMonto() { return monto; }
	public void setMonto(Double monto) { this.monto = monto; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public Long getIdLineaCruce() { return idLineaCruce; }
	public void setIdLineaCruce(Long idLineaCruce) { this.idLineaCruce = idLineaCruce; }
	public LocalDate getFechaHecho() { return fechaHecho; }
	public void setFechaHecho(LocalDate fechaHecho) { this.fechaHecho = fechaHecho; }
	public String getTipoDocumento() { return tipoDocumento; }
	public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
	public String getNumeroDocumento() { return numeroDocumento; }
	public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
	public Long getIdDecision() { return idDecision; }
	public void setIdDecision(Long idDecision) { this.idDecision = idDecision; }
	public String getTexto() { return texto; }
	public void setTexto(String texto) { this.texto = texto; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
	public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
	public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }
	public String getUsuarioAnulacion() { return usuarioAnulacion; }
	public void setUsuarioAnulacion(String usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }
	public String getMotivoAnulacion() { return motivoAnulacion; }
	public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
}
