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
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * C25 — Decisión (CNT.DCSN): las D-nnn del expediente (origen EXPEDIENTE) y las que registra Contabilidad de ASOPREP en la pantalla de seguimiento de novedades (origen SAA, con constancia en CNT.ADNV). D-056 (3): las decisiones viven en los dos lados.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C25-01-seguimiento-novedades.sql. Las FK se guardan como id (no relación):
 * la entidad se serializa directo a JSON y la pantalla arma el detalle con GET /nvdd/{id}/detalle.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DCSN", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "DecisionAll", query = "select e from Decision e"),
	@NamedQuery(name = "DecisionId", query = "select e from Decision e where e.codigo = :id")
})
public class Decision implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "DCSNCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Empresa (SCP.PJRQ). */
	@Basic
	@Column(name = "PJRQCDGO")
	private Long idEmpresa;

	/** Número legible: D-056 (único por empresa). */
	@Basic
	@Column(name = "DCSNNMRO", length = 10)
	private String numero;

	/** Fecha de la decisión. */
	@Basic
	@Column(name = "DCSNFCHA")
	private LocalDate fecha;

	/** Tema. */
	@Basic
	@Column(name = "DCSNTEMA", length = 1000)
	private String tema;

	/** Texto completo (CLOB). */
	@Lob
	@Column(name = "DCSNDTLL")
	private String detalle;

	/** CONTABILIDAD_ASOPREP, RESPONSABLE_ASOPREP, GERENCIA, LIDER_PROYECTO, GAEMII. */
	@Basic
	@Column(name = "DCSNTPDC", length = 25)
	private String tipoDecide;

	/** Cargo de quien decide (nunca el nombre). */
	@Basic
	@Column(name = "DCSNCRGO", length = 200)
	private String cargo;

	/** Evidencia (correo, acta, reunión). */
	@Basic
	@Column(name = "DCSNEVDN", length = 1000)
	private String evidencia;

	/** EXPEDIENTE o SAA. */
	@Basic
	@Column(name = "DCSNORGN", length = 20)
	private String origen;

	/** Cuándo se copió a DECISIONES-Y-PARTIDAS.md. */
	@Basic
	@Column(name = "DCSNFCEX")
	private LocalDateTime fechaCopiaExpediente;

	/** Quién marcó la copia al expediente. */
	@Basic
	@Column(name = "DCSNUSEX", length = 50)
	private String usuarioCopiaExpediente;

	/** Fecha de registro. */
	@Basic
	@Column(name = "DCSNFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que registró. */
	@Basic
	@Column(name = "DCSNUSAR", length = 50)
	private String usuarioRegistro;

	/** 1 vigente, 2 reemplazada, 3 anulada. */
	@Basic
	@Column(name = "DCSNESTD")
	private Long estado;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdEmpresa() { return idEmpresa; }
	public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
	public String getNumero() { return numero; }
	public void setNumero(String numero) { this.numero = numero; }
	public LocalDate getFecha() { return fecha; }
	public void setFecha(LocalDate fecha) { this.fecha = fecha; }
	public String getTema() { return tema; }
	public void setTema(String tema) { this.tema = tema; }
	public String getDetalle() { return detalle; }
	public void setDetalle(String detalle) { this.detalle = detalle; }
	public String getTipoDecide() { return tipoDecide; }
	public void setTipoDecide(String tipoDecide) { this.tipoDecide = tipoDecide; }
	public String getCargo() { return cargo; }
	public void setCargo(String cargo) { this.cargo = cargo; }
	public String getEvidencia() { return evidencia; }
	public void setEvidencia(String evidencia) { this.evidencia = evidencia; }
	public String getOrigen() { return origen; }
	public void setOrigen(String origen) { this.origen = origen; }
	public LocalDateTime getFechaCopiaExpediente() { return fechaCopiaExpediente; }
	public void setFechaCopiaExpediente(LocalDateTime fechaCopiaExpediente) { this.fechaCopiaExpediente = fechaCopiaExpediente; }
	public String getUsuarioCopiaExpediente() { return usuarioCopiaExpediente; }
	public void setUsuarioCopiaExpediente(String usuarioCopiaExpediente) { this.usuarioCopiaExpediente = usuarioCopiaExpediente; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }

	// C25-02 — revisión con el cliente
	/** Título corto. */
	@Basic
	@Column(name = "DCSNTTLO", length = 300)
	private String titulo;
	/** Resumen en una frase. */
	@Basic
	@Column(name = "DCSNRSMN", length = 2000)
	private String resumen;
	/** Razones (por qué se decidió así). */
	@Basic
	@Column(name = "DCSNRZNS", length = 2000)
	private String razones;
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public String getResumen() { return resumen; }
	public void setResumen(String resumen) { this.resumen = resumen; }
	public String getRazones() { return razones; }
	public void setRazones(String razones) { this.razones = razones; }
}
