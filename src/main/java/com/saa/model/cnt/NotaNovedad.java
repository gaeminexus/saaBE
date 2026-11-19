package com.saa.model.cnt;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * C34d — Nota de ASOPREP en una novedad (CNT.NTNV): respuesta escrita sin cuentas, que no cambia saldos ni estados. Se puede
 * convertir después en un hecho de crédito (HCNVCDGO).
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C34-07b-hechos-serie-y-notas.sql.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NTNV", schema = "CNT")
public class NotaNovedad implements Serializable {

	@Id
	@Basic
	@Column(name = "NTNVCDGO")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;
	@Basic
	@Column(name = "NVDDCDGO")
	private Long idNovedad;
	@Basic
	@Column(name = "NTNVTXTO", length = 4000)
	private String texto;
	@Basic
	@Column(name = "HCNVCDGO")
	private Long idHecho;
	/** 1 VIGENTE, 2 ANULADA. */
	@Basic
	@Column(name = "NTNVESTD")
	private Long estado;
	@Basic
	@Column(name = "NTNVFCRG")
	private LocalDateTime fechaRegistro;
	@Basic
	@Column(name = "NTNVUSAR", length = 50)
	private String usuarioRegistro;
	@Basic
	@Column(name = "NTNVFCAN")
	private LocalDateTime fechaAnulacion;
	@Basic
	@Column(name = "NTNVUSAN", length = 50)
	private String usuarioAnulacion;
	@Basic
	@Column(name = "NTNVMTAN", length = 1000)
	private String motivoAnulacion;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdNovedad() { return idNovedad; }
	public void setIdNovedad(Long idNovedad) { this.idNovedad = idNovedad; }
	public String getTexto() { return texto; }
	public void setTexto(String texto) { this.texto = texto; }
	public Long getIdHecho() { return idHecho; }
	public void setIdHecho(Long idHecho) { this.idHecho = idHecho; }
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
