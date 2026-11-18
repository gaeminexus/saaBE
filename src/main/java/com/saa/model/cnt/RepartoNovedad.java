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
import java.time.LocalDateTime;

/**
 * C34 — Reparto del valor justificado (o respondido) a una cuenta contable (CNT.RPNV): lo que la pasada v8 del re-proceso registra en lugar de la cuenta por regularizar.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C34-01-novedades-v2.sql. Las FK se guardan como id (no relación), como en C25.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RPNV", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "RepartoNovedadAll", query = "select e from RepartoNovedad e"),
	@NamedQuery(name = "RepartoNovedadId", query = "select e from RepartoNovedad e where e.codigo = :id")
})
public class RepartoNovedad implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "RPNVCDGO")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Justificación dueña (o respuesta). */
	@Basic
	@Column(name = "JSNVCDGO")
	private Long idJustificacion;

	/** Respuesta dueña (o justificación). */
	@Basic
	@Column(name = "RSNVCDGO")
	private Long idRespuesta;

	/** Cuenta destino (CNT.PLNN). */
	@Basic
	@Column(name = "PLNNCDGO")
	private Long idCuenta;

	/** Valor. */
	@Basic
	@Column(name = "RPNVVLOR")
	private Double valor;

	/** D débito / H crédito en la cuenta destino. */
	@Basic
	@Column(name = "RPNVSNTD", length = 1)
	private String sentido;

	/** Préstamo (opcional). */
	@Basic
	@Column(name = "PRSTCDGO")
	private Long idPrestamo;

	/** Cuota (opcional). */
	@Basic
	@Column(name = "DTPRCDGO")
	private Long idCuota;

	/** Partícipe (opcional). */
	@Basic
	@Column(name = "ENTDCDGO")
	private Long idEntidad;

	/** Glosa. */
	@Basic
	@Column(name = "RPNVGLSA", length = 500)
	private String glosa;

	/** Fecha de registro. */
	@Basic
	@Column(name = "RPNVFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que registró. */
	@Basic
	@Column(name = "RPNVUSAR", length = 50)
	private String usuarioRegistro;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdJustificacion() { return idJustificacion; }
	public void setIdJustificacion(Long idJustificacion) { this.idJustificacion = idJustificacion; }
	public Long getIdRespuesta() { return idRespuesta; }
	public void setIdRespuesta(Long idRespuesta) { this.idRespuesta = idRespuesta; }
	public Long getIdCuenta() { return idCuenta; }
	public void setIdCuenta(Long idCuenta) { this.idCuenta = idCuenta; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public String getSentido() { return sentido; }
	public void setSentido(String sentido) { this.sentido = sentido; }
	public Long getIdPrestamo() { return idPrestamo; }
	public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
	public Long getIdCuota() { return idCuota; }
	public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }
	public Long getIdEntidad() { return idEntidad; }
	public void setIdEntidad(Long idEntidad) { this.idEntidad = idEntidad; }
	public String getGlosa() { return glosa; }
	public void setGlosa(String glosa) { this.glosa = glosa; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
