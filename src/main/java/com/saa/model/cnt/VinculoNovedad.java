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
 * C25 — Vínculo de una novedad (CNT.VNNV): asiento de origen, ASIENTO DE AJUSTE manual con que Contabilidad arregló la novedad (uno o varios, D-056 (7)), partida de C22, decisión, otra novedad o documento de otro módulo. Se anula, no se borra.
 *
 * <p>DDL: docs/logica-negocio/cnt/sql/C25-01-seguimiento-novedades.sql. Las FK se guardan como id (no relación):
 * la entidad se serializa directo a JSON y la pantalla arma el detalle con GET /nvdd/{id}/detalle.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "VNNV", schema = "CNT")
@NamedQueries({
	@NamedQuery(name = "VinculoNovedadAll", query = "select e from VinculoNovedad e"),
	@NamedQuery(name = "VinculoNovedadId", query = "select e from VinculoNovedad e where e.codigo = :id")
})
public class VinculoNovedad implements Serializable {

	/** PK (IDENTITY). */
	@Id
	@Basic
	@Column(name = "VNNVCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long codigo;

	/** Novedad. */
	@Basic
	@Column(name = "NVDDCDGO")
	private Long idNovedad;

	/** Requerimiento (opcional). */
	@Basic
	@Column(name = "RQNVCDGO")
	private Long idRequerimiento;

	/** ASIENTO_ORIGEN, ASIENTO_AJUSTE, PARTIDA_C22, DECISION, NOVEDAD, DOCUMENTO_MODULO. */
	@Basic
	@Column(name = "VNNVTPVN", length = 20)
	private String tipo;

	/** Asiento (CNT.ASNT). */
	@Basic
	@Column(name = "ASNTCDGO")
	private Long idAsiento;

	/** Línea de asiento (CNT.DTAS). */
	@Basic
	@Column(name = "DTASCDGO")
	private Long idDetalleAsiento;

	/** Partida por regularizar de C22 (sin FK hasta que C22 exista). */
	@Basic
	@Column(name = "PRRGCDGO")
	private Long idPartidaC22;

	/** Decisión. */
	@Basic
	@Column(name = "DCSNCDGO")
	private Long idDecision;

	/** Novedad relacionada. */
	@Basic
	@Column(name = "VNNVNVRL")
	private Long idNovedadRelacionada;

	/** Tipo de documento de otro módulo. */
	@Basic
	@Column(name = "VNNVTPDM", length = 10)
	private String tipoDocumentoModulo;

	/** Código del documento de otro módulo. */
	@Basic
	@Column(name = "VNNVIDDM")
	private Long idDocumentoModulo;

	/** Valor vinculado. */
	@Basic
	@Column(name = "VNNVVLOR")
	private Double valor;

	/** Observaciones. */
	@Basic
	@Column(name = "VNNVOBSR", length = 1000)
	private String observacion;

	/** Fecha de registro. */
	@Basic
	@Column(name = "VNNVFCRG")
	private LocalDateTime fechaRegistro;

	/** Usuario que registró. */
	@Basic
	@Column(name = "VNNVUSAR", length = 50)
	private String usuarioRegistro;

	/** 1 activo, 2 anulado. */
	@Basic
	@Column(name = "VNNVESTD")
	private Long estado;

	/** Fecha de anulación. */
	@Basic
	@Column(name = "VNNVFCAN")
	private LocalDateTime fechaAnulacion;

	/** Usuario que anuló. */
	@Basic
	@Column(name = "VNNVUSAN", length = 50)
	private String usuarioAnulacion;

	/** Motivo de la anulación. */
	@Basic
	@Column(name = "VNNVMTAN", length = 1000)
	private String motivoAnulacion;

	public Long getCodigo() { return codigo; }
	public void setCodigo(Long codigo) { this.codigo = codigo; }
	public Long getIdNovedad() { return idNovedad; }
	public void setIdNovedad(Long idNovedad) { this.idNovedad = idNovedad; }
	public Long getIdRequerimiento() { return idRequerimiento; }
	public void setIdRequerimiento(Long idRequerimiento) { this.idRequerimiento = idRequerimiento; }
	public String getTipo() { return tipo; }
	public void setTipo(String tipo) { this.tipo = tipo; }
	public Long getIdAsiento() { return idAsiento; }
	public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }
	public Long getIdDetalleAsiento() { return idDetalleAsiento; }
	public void setIdDetalleAsiento(Long idDetalleAsiento) { this.idDetalleAsiento = idDetalleAsiento; }
	public Long getIdPartidaC22() { return idPartidaC22; }
	public void setIdPartidaC22(Long idPartidaC22) { this.idPartidaC22 = idPartidaC22; }
	public Long getIdDecision() { return idDecision; }
	public void setIdDecision(Long idDecision) { this.idDecision = idDecision; }
	public Long getIdNovedadRelacionada() { return idNovedadRelacionada; }
	public void setIdNovedadRelacionada(Long idNovedadRelacionada) { this.idNovedadRelacionada = idNovedadRelacionada; }
	public String getTipoDocumentoModulo() { return tipoDocumentoModulo; }
	public void setTipoDocumentoModulo(String tipoDocumentoModulo) { this.tipoDocumentoModulo = tipoDocumentoModulo; }
	public Long getIdDocumentoModulo() { return idDocumentoModulo; }
	public void setIdDocumentoModulo(Long idDocumentoModulo) { this.idDocumentoModulo = idDocumentoModulo; }
	public Double getValor() { return valor; }
	public void setValor(Double valor) { this.valor = valor; }
	public String getObservacion() { return observacion; }
	public void setObservacion(String observacion) { this.observacion = observacion; }
	public LocalDateTime getFechaRegistro() { return fechaRegistro; }
	public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
	public String getUsuarioRegistro() { return usuarioRegistro; }
	public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
	public Long getEstado() { return estado; }
	public void setEstado(Long estado) { this.estado = estado; }
	public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
	public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }
	public String getUsuarioAnulacion() { return usuarioAnulacion; }
	public void setUsuarioAnulacion(String usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }
	public String getMotivoAnulacion() { return motivoAnulacion; }
	public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
}
